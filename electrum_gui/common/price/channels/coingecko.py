import logging
from typing import Iterable

import peewee

from electrum_gui.common.basic.request.restful import RestfulRequest
from electrum_gui.common.coin import codes
from electrum_gui.common.coin.data import CoinInfo
from electrum_gui.common.conf import settings
from electrum_gui.common.price.data import YieldedPrice
from electrum_gui.common.price.interfaces import PriceChannelInterface

logger = logging.getLogger("app.price")


class Coingecko(PriceChannelInterface):
    def __init__(self, url: str):
        self.restful = RestfulRequest(url)

    def fetch_btc_to_fiats(self) -> Iterable[YieldedPrice]:
        resp = self.restful.get("/api/v3/exchange_rates")
        rates = resp.get("rates") or {}

        rates = ((unit, rate) for unit, rate in rates.items() if rate and rate.get("type") == "fiat")
        rates = (YieldedPrice(coin_code=codes.BTC, unit=unit, price=rate.get("value") or 0) for unit, rate in rates)
        yield from rates

    def fetch_cgk_ids_to_currency(self, currency: str) -> Iterable[YieldedPrice]:
        reversed_ids = {cgk_id: code for code, cgk_id in settings.COINGECKO_IDS.items()}
        resp = (
            self.restful.get(
                "/api/v3/coins/markets",
                params={
                    "ids": ",".join(reversed_ids.keys()),
                    "vs_currency": currency,
                },
            )
            or ()
        )

        rates = (rate for rate in resp if rate and rate.get("id") in reversed_ids)
        rates = (
            YieldedPrice(coin_code=reversed_ids[rate.get("id")], unit=currency, price=rate.get("current_price") or 0)
            for rate in rates
        )
        yield from rates

    def fetch_erc20_to_currency(self, erc20_coins: Iterable[CoinInfo], currency: str) -> Iterable[YieldedPrice]:
        mapping = {i.token_address.lower(): i for i in erc20_coins}

        for batch_addresses in peewee.chunked(mapping.keys(), 100):
            resp = self.restful.get(
                "/api/v3/simple/token_price/ethereum",
                params={
                    "contract_addresses": ",".join(batch_addresses),
                    "vs_currencies": currency,
                },
            )
            rates = ((address.lower(), rate) for address, rate in resp.items() if address.lower() in mapping and rate)
            rates = (
                YieldedPrice(
                    coin_code=mapping[address].code,
                    unit=currency,
                    price=rate.get(currency) or 0,
                )
                for address, rate in rates
            )
            yield from rates

    def pricing(self, coins: Iterable[CoinInfo]) -> Iterable[YieldedPrice]:
        try:
            yield from self.fetch_btc_to_fiats()
        except Exception as e:
            logger.exception(f"Error in fetching fiat rate of btc. error: {e}")

        try:
            yield from self.fetch_cgk_ids_to_currency(currency=codes.BTC)
        except Exception as e:
            logger.exception(f"Error in fetching btc rate of config cgk ids. error: {e}")

        try:
            erc20_coins = (coin for coin in coins if coin.token_address and coin.chain_code == codes.ETH)
            yield from self.fetch_erc20_to_currency(erc20_coins, currency=codes.BTC)
        except Exception as e:
            logger.exception(f"Error in fetching btc rate of erc20. error: {e}")
