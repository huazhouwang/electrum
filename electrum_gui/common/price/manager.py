import functools
import logging
from decimal import Decimal
from typing import Callable, Dict, Iterable, Sequence

from electrum_gui.common.coin import codes
from electrum_gui.common.coin import manager as coin_manager
from electrum_gui.common.conf import settings
from electrum_gui.common.price import daos
from electrum_gui.common.price.channels.coingecko import Coingecko
from electrum_gui.common.price.data import Channel, NumericType
from electrum_gui.common.price.interfaces import PriceChannelInterface

logger = logging.getLogger("app.price")

_registry: Dict[Channel, Callable[[], PriceChannelInterface]] = {
    Channel.cgk: functools.partial(Coingecko, settings.COINGECKO_API_HOST),
}


def pricing():
    coins = coin_manager.get_all_coins()

    for channel_type, channel_creator in _registry.items():
        try:
            channel = channel_creator()

            for price in channel.pricing(coins):
                daos.create_or_update(
                    coin_code=price.coin_code,
                    unit=price.unit,
                    channel=channel_type,
                    price=price.price,
                )
        except Exception as e:
            logger.exception(f"Error in running channel. channel_type: {channel_type}, error: {e}")


def get_last_price(coin_code: str, unit: str, default: NumericType = 0) -> Decimal:
    coin_code = settings.PRICING_COIN_MAPPING.get(coin_code) or coin_code
    unit = settings.PRICING_COIN_MAPPING.get(unit) or unit

    price = 0
    for path in _generate_searching_paths(coin_code, unit):
        if len(path) < 2:
            continue

        price = 1
        for i in range(len(path) - 1):
            input_code, output_code = path[i].lower(), path[i + 1].lower()
            if input_code == output_code:
                rate = 1
            else:
                rate = daos.get_last_price(input_code, output_code)
                if rate <= 0:
                    reversed_rate = daos.get_last_price(output_code, input_code)
                    rate = 1 / reversed_rate if reversed_rate > 0 else rate

            price *= rate
            if price <= 0:  # got invalid path
                break

        if price > 0:  # got price already
            break

    price = price if price > 0 else default
    return Decimal(str(price))


def _generate_searching_paths(coin_code: str, unit: str) -> Iterable[Sequence[str]]:
    yield coin_code, unit

    if coin_code == codes.BTC or unit == codes.BTC:
        return

    yield coin_code, codes.BTC, unit

    coin = coin_manager.get_coin_info(coin_code, nullable=True)
    if coin and coin.chain_code not in (coin_code, unit, codes.BTC):
        yield coin_code, coin.chain_code, unit
        yield coin_code, coin.chain_code, codes.BTC, unit
