"""
Card data structure and RPS rules engine.
Designed for easy expansion with future cards, rarities, and mechanics.
"""

from enum import Enum
from typing import List, Dict, Any, Optional

class CardType(Enum):
    ROCK = "rock"
    PAPER = "paper"
    SCISSORS = "scissors"

    @classmethod
    def from_str(cls, val: str) -> Optional['CardType']:
        clean = val.strip().lower()
        for member in cls:
            if member.value == clean or member.name.lower() == clean:
                return member
        return None

class WagerRank(Enum):
    C = (3, "#FFFFFF", "White/Gray")
    B = (7, "#00E5FF", "Cyan")
    A = (15, "#FFD700", "Yellow/Gold")
    S = (25, "#FF0055", "Magenta/Neon Red")

    def __init__(self, points: int, hex_color: str, color_name: str):
        self.points = points
        self.hex_color = hex_color
        self.color_name = color_name

    @classmethod
    def from_str(cls, val: str) -> Optional['WagerRank']:
        clean = val.strip().upper()
        for member in cls:
            if member.name == clean:
                return member
        return None

class Card:
    def __init__(self, card_id: str, card_type: CardType, rarity: str = "COMMON"):
        self.id = card_id
        self.type = card_type
        self.rarity = rarity

    @property
    def display_name(self) -> str:
        return self.type.name  # "ROCK", "PAPER", "SCISSORS"

    @property
    def terminal_color(self) -> str:
        # ROCK -> Gray/White, PAPER -> Cyan/Blue, SCISSORS -> Red/Orange
        if self.type == CardType.ROCK:
            return "#E0E0E0"
        elif self.type == CardType.PAPER:
            return "#00E5FF"
        elif self.type == CardType.SCISSORS:
            return "#FF5252"
        return "#FFFFFF"

    def to_dict(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "type": self.type.value,
            "display_name": self.display_name,
            "color": self.terminal_color,
            "rarity": self.rarity
        }

def create_starting_deck() -> List[Card]:
    """
    Initial deck: 2 ROCK, 2 PAPER, 2 SCISSORS (Total: 6 cards)
    """
    cards = []
    idx = 1
    for _ in range(2):
        cards.append(Card(f"rock_{idx}", CardType.ROCK))
        idx += 1
    for _ in range(2):
        cards.append(Card(f"paper_{idx}", CardType.PAPER))
        idx += 1
    for _ in range(2):
        cards.append(Card(f"scissors_{idx}", CardType.SCISSORS))
        idx += 1
    return cards

def determine_winner(card1_type: CardType, card2_type: CardType) -> int:
    """
    Standard RPS rules:
    ROCK beats SCISSORS
    SCISSORS beats PAPER
    PAPER beats ROCK
    Returns:
      0 if draw
      1 if card1 wins
      2 if card2 wins
    """
    if card1_type == card2_type:
        return 0

    if (card1_type == CardType.ROCK and card2_type == CardType.SCISSORS) or \
       (card1_type == CardType.SCISSORS and card2_type == CardType.PAPER) or \
       (card1_type == CardType.PAPER and card2_type == CardType.ROCK):
        return 1
    else:
        return 2

def get_outcome_reason(card1_type: CardType, card2_type: CardType) -> str:
    if card1_type == card2_type:
        return f"BOTH PLAYED {card1_type.name}. IT'S A DRAW!"
    w = determine_winner(card1_type, card2_type)
    if w == 1:
        return f"{card1_type.name} BEATS {card2_type.name}!"
    else:
        return f"{card2_type.name} BEATS {card1_type.name}!"
