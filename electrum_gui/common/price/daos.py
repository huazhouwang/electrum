import datetime
from decimal import Decimal

from electrum_gui.common.price.data import Channel, NumericType
from electrum_gui.common.price.models import Price


def create_or_update(
    coin_code: str,
    unit: str,
    channel: Channel,
    price: NumericType,
):
    price = Decimal(str(price))
    model, is_newborn = Price.get_or_create(
        coin_code=coin_code,
        unit=unit,
        channel=channel,
        defaults=dict(price=price),
    )

    if not is_newborn:
        Price.update(
            price=price,
            modified_time=datetime.datetime.now(),
        ).where(Price.id == model.id).execute()


def get_last_price(
    coin_code: str,
    unit: str,
    default: NumericType = 0,
    channel: Channel = None,
) -> Decimal:
    query = [Price.coin_code == coin_code, Price.unit == unit]
    if channel is not None:
        query.append(Price.channel == channel)

    model = Price.select().where(*query).order_by(Price.modified_time.desc()).first()
    return model.price if model else Decimal(str(default))
