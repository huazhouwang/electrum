from collections import namedtuple
from decimal import Decimal
from enum import IntEnum, unique
from typing import Union


@unique
class Channel(IntEnum):
    cgk = 10

    @classmethod
    def to_choices(cls):
        return ((cls.cgk, "Coingecko"),)


NumericType = Union[int, float, str, Decimal]

YieldedPrice = namedtuple("YieldedPrice", ["coin_code", "price", "unit"])
