import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from typing import List, Optional, Any
from card import Card, CardType, WagerRank, create_starting_deck

class Player:
    def __init__(self, player_id: str, name: str):
        self.id: str = player_id
        self.name: str = name
        self.score: int = 50
        self.cards: List[Card] = create_starting_deck()
        self.is_ready: bool = False
        self.pending_card_type: Optional[CardType] = None
        self.chosen_card: Optional[Card] = None
        self.chosen_rank: Optional[WagerRank] = None
        self.websocket: Any = None
        self.is_connected: bool = True

    def count_cards(self) -> dict:
        counts = {CardType.ROCK: 0, CardType.PAPER: 0, CardType.SCISSORS: 0}
        for c in self.cards:
            counts[c.type] += 1
        return counts

    def has_card_type(self, card_type: CardType) -> bool:
        return any(c.type == card_type for c in self.cards)

    def can_afford_wager(self, rank: WagerRank) -> bool:
        return self.score >= rank.points

    def play_card(self, card_type: CardType, rank: WagerRank) -> Optional[Card]:
        """
        Removes the card from player's inventory and sets chosen card & rank.
        Checks if player deck is exhausted; if so, replenishes after round.
        """
        for i, c in enumerate(self.cards):
            if c.type == card_type:
                played = self.cards.pop(i)
                self.chosen_card = played
                self.chosen_rank = rank
                return played
        return None

    def replenish_deck_if_empty(self) -> bool:
        if len(self.cards) == 0:
            self.cards = create_starting_deck()
            return True
        return False

    def reset_round_choice(self):
        self.pending_card_type = None
        self.chosen_card = None
        self.chosen_rank = None
        self.is_ready = False

    def format_cards_bracket(self) -> str:
        """
        Formats remaining cards in bracket notation:
        [ROCK] [ROCK]
        [PAPER] [PAPER]
        [SCISSORS] [SCISSORS]
        """
        counts = self.count_cards()
        lines = []
        if counts[CardType.ROCK] > 0:
            lines.append(" ".join(["[ROCK]"] * counts[CardType.ROCK]))
        else:
            lines.append("[ROCK] (EMPTY)")

        if counts[CardType.PAPER] > 0:
            lines.append(" ".join(["[PAPER]"] * counts[CardType.PAPER]))
        else:
            lines.append("[PAPER] (EMPTY)")

        if counts[CardType.SCISSORS] > 0:
            lines.append(" ".join(["[SCISSORS]"] * counts[CardType.SCISSORS]))
        else:
            lines.append("[SCISSORS] (EMPTY)")

        return "\n".join(lines)
