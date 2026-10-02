import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'shared'))

import asyncio
from typing import Dict, Any, Optional, Tuple, Callable
from card import CardType, WagerRank, determine_winner, get_outcome_reason
from player import Player
from protocol import (
    STATE_WAITING_FOR_PLAYERS,
    STATE_CONNECTED,
    STATE_WAITING_FOR_START,
    STATE_ROUND_START,
    STATE_WAITING_FOR_CHOICES,
    STATE_BOTH_CHOICES_LOCKED,
    STATE_REVEAL,
    STATE_RESOLVE,
    STATE_UPDATE_SCORE,
    STATE_NEXT_ROUND
)

class RPSGame:
    def __init__(self, broadcast_fn: Callable, send_to_player_fn: Callable):
        self.broadcast = broadcast_fn
        self.send_to_player = send_to_player_fn
        self.player1: Optional[Player] = None
        self.player2: Optional[Player] = None
        self.state: str = STATE_WAITING_FOR_PLAYERS
        self.round_number: int = 1
        self.reveal_task: Optional[asyncio.Task] = None

    def get_player(self, player_id: str) -> Optional[Player]:
        if self.player1 and self.player1.id == player_id:
            return self.player1
        if self.player2 and self.player2.id == player_id:
            return self.player2
        return None

    def get_opponent(self, player_id: str) -> Optional[Player]:
        if self.player1 and self.player1.id == player_id:
            return self.player2
        if self.player2 and self.player2.id == player_id:
            return self.player1
        return None

    def are_both_connected(self) -> bool:
        return (self.player1 is not None and self.player1.is_connected and
                self.player2 is not None and self.player2.is_connected)

    async def add_player(self, player_id: str, name: str, websocket: Any) -> Tuple[Player, str]:
        if self.player1 is None:
            self.player1 = Player(player_id, name)
            self.player1.websocket = websocket
            role = "player1"
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] Welcome, {name} (PLAYER 1 - HOST). Waiting for Player 2 to connect...",
                "level": "info"
            })
            self.state = STATE_WAITING_FOR_PLAYERS
            await self.sync_state()
            return self.player1, role
        elif self.player2 is None:
            self.player2 = Player(player_id, name)
            self.player2.websocket = websocket
            role = "player2"
            self.state = STATE_WAITING_FOR_START
            await self.broadcast({
                "type": "system",
                "text": f"[SYSTEM] {name} connected as PLAYER 2.\n[SYSTEM] Both players connected! Type /start when ready.",
                "level": "success"
            })
            await self.sync_state()
            return self.player2, role
        else:
            # Check reconnection
            if self.player1 and not self.player1.is_connected:
                self.player1.id = player_id
                self.player1.websocket = websocket
                self.player1.is_connected = True
                await self.broadcast({
                    "type": "system",
                    "text": f"[SYSTEM] PLAYER 1 RECONNECTED.",
                    "level": "info"
                })
                await self.sync_state()
                return self.player1, "player1"
            elif self.player2 and not self.player2.is_connected:
                self.player2.id = player_id
                self.player2.websocket = websocket
                self.player2.is_connected = True
                await self.broadcast({
                    "type": "system",
                    "text": f"[SYSTEM] PLAYER 2 RECONNECTED.",
                    "level": "info"
                })
                await self.sync_state()
                return self.player2, "player2"
            else:
                return None, "spectator"

    async def handle_disconnect(self, player_id: str):
        player = self.get_player(player_id)
        if player:
            player.is_connected = False
            player.websocket = None
            role_name = "PLAYER 1" if player == self.player1 else "PLAYER 2"
            await self.broadcast({
                "type": "system",
                "text": f"[SYSTEM] {role_name} DISCONNECTED.\nWAITING FOR RECONNECTION...",
                "level": "danger"
            })
            await self.sync_state()

    async def set_player_ready(self, player_id: str):
        if not self.are_both_connected():
            await self.send_to_player(player_id, {
                "type": "system",
                "text": "[SYSTEM] Cannot start: Waiting for opponent to connect.",
                "level": "warning"
            })
            return

        if self.state not in [STATE_WAITING_FOR_START, STATE_NEXT_ROUND, STATE_CONNECTED]:
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] Round already in progress (State: {self.state}).",
                "level": "warning"
            })
            return

        player = self.get_player(player_id)
        if not player:
            return

        player.is_ready = True
        role_label = "PLAYER 1" if player == self.player1 else "PLAYER 2"
        await self.broadcast({
            "type": "system",
            "text": f"[SYSTEM] {role_label} is ready.",
            "level": "info"
        })

        opponent = self.get_opponent(player_id)
        if opponent and not opponent.is_ready:
            opp_label = "PLAYER 2" if player == self.player1 else "PLAYER 1"
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] Waiting for {opp_label} to /start...",
                "level": "info"
            })

        if self.player1.is_ready and self.player2.is_ready:
            await self.start_round()

        await self.sync_state()

    async def start_round(self):
        self.state = STATE_WAITING_FOR_CHOICES
        self.player1.reset_round_choice()
        self.player2.reset_round_choice()

        banner = (
            f"\n========================================\n"
            f"   ROUND {self.round_number} STARTING\n"
            f"   BOTH PLAYERS READY\n"
            f"========================================\n"
        )
        await self.broadcast({
            "type": "system",
            "text": banner,
            "level": "success"
        })

        # Send each player their remaining cards and the prompt
        for player in [self.player1, self.player2]:
            if player:
                prompt_msg = (
                    f"YOUR AVAILABLE CARDS:\n"
                    f"{player.format_cards_bracket()}\n\n"
                    f"Type the card you want to play: rock, paper, or scissors\n"
                    f"(Or enter both: e.g. rock A)"
                )
                await self.send_to_player(player.id, {
                    "type": "system",
                    "text": prompt_msg,
                    "level": "cards_display"
                })

        await self.sync_state()

    async def handle_conversational_input(self, player_id: str, text: str) -> bool:
        """
        Handles step-by-step card and rank input during WAITING_FOR_CHOICES.
        Returns True if the input was consumed as game choice, False if it should be treated as chat.
        """
        if self.state != STATE_WAITING_FOR_CHOICES:
            return False

        player = self.get_player(player_id)
        if not player or player.chosen_card is not None:
            return False

        clean = text.strip()
        parts = clean.split()

        # Case 1: Player enters both card and rank at once (e.g. "rock A" or "paper B")
        if len(parts) == 2:
            card_cand = CardType.from_str(parts[0])
            rank_cand = WagerRank.from_str(parts[1])
            if card_cand and rank_cand:
                await self.process_choice(player_id, parts[0], parts[1])
                return True

        # Case 2: Player enters just the card (e.g. "rock", "paper", "scissors")
        if len(parts) == 1:
            card_type = CardType.from_str(parts[0])
            if card_type:
                if not player.has_card_type(card_type):
                    await self.send_to_player(player_id, {
                        "type": "system",
                        "text": f"[SYSTEM] You have no remaining {card_type.name} cards! Choose from available cards.",
                        "level": "danger"
                    })
                    return True

                player.pending_card_type = card_type
                await self.send_to_player(player_id, {
                    "type": "system",
                    "text": (
                        f"[CARD SELECTED: {card_type.name}]\n"
                        f"Now choose a wager rank:\n"
                        f"  C  (3 points)   - Minimal risk\n"
                        f"  B  (7 points)   - Standard wager\n"
                        f"  A  (15 points)  - High stakes\n"
                        f"  S  (25 points)  - Supreme gamble\n"
                        f"Type: C, B, A, or S (Your score: {player.score})"
                    ),
                    "level": "info"
                })
                return True

        # Case 3: Player already selected card, now enters rank (e.g. "A", "B", "C", "S")
        if len(parts) == 1 and player.pending_card_type is not None:
            rank = WagerRank.from_str(parts[0])
            if rank:
                await self.process_choice(player_id, player.pending_card_type.value, parts[0])
                return True

        return False

    async def process_choice(self, player_id: str, card_str: str, rank_str: str):
        if self.state != STATE_WAITING_FOR_CHOICES:
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] No active round for choosing.\nUse /start when ready.",
                "level": "warning"
            })
            return

        player = self.get_player(player_id)
        if not player:
            return

        if player.chosen_card is not None:
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] You already locked in your card for this round.",
                "level": "warning"
            })
            return

        card_type = CardType.from_str(card_str)
        if not card_type:
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] Invalid card '{card_str}'. Choose from: rock, paper, scissors.",
                "level": "danger"
            })
            return

        rank = WagerRank.from_str(rank_str)
        if not rank:
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] Invalid rank '{rank_str}'. Available ranks: C (3pts), B (7pts), A (15pts), S (25pts).",
                "level": "danger"
            })
            return

        if not player.has_card_type(card_type):
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] You have no remaining {card_type.name} cards! Check /cards.",
                "level": "danger"
            })
            return

        if not player.can_afford_wager(rank):
            await self.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] Cannot afford wager {rank.name} ({rank.points} pts)! Your score is {player.score}.",
                "level": "danger"
            })
            return

        # Play card and attach rank
        played = player.play_card(card_type, rank)
        player.pending_card_type = None
        role_label = "PLAYER 1" if player == self.player1 else "PLAYER 2"

        # Local player sees full confirmation
        await self.send_to_player(player_id, {
            "type": "system",
            "text": f"[SYSTEM] Card locked: [{card_type.name}    {rank.name}] (Wager: {rank.points} pts)",
            "level": "success"
        })

        # Opponent sees ONLY the rank and hidden type!
        opponent = self.get_opponent(player_id)
        if opponent:
            await self.send_to_player(opponent.id, {
                "type": "system",
                "text": f"[SYSTEM] {role_label} locked card: [ ??????  {rank.name} ]",
                "level": "info"
            })

        await self.sync_state()

        # Check if both have locked
        if self.player1.chosen_card and self.player2.chosen_card:
            self.state = STATE_BOTH_CHOICES_LOCKED
            await self.sync_state()
            asyncio.create_task(self.run_reveal_sequence())

    async def run_reveal_sequence(self):
        """
        Reveal sequence:
        Compact aesthetic countdown, duel box, and individual chosen wager resolution.
        """
        self.state = STATE_REVEAL
        await self.broadcast({
            "type": "system",
            "text": "\n[SYSTEM] BOTH CARDS LOCKED.",
            "level": "info"
        })

        await asyncio.sleep(0.4)
        await self.broadcast({"type": "system", "text": "[ 3 • 2 • 1 • REVEAL! ]", "level": "highlight"})
        await asyncio.sleep(0.4)

        p1_card = self.player1.chosen_card
        p1_rank = self.player1.chosen_rank
        p2_card = self.player2.chosen_card
        p2_rank = self.player2.chosen_rank

        # Compact aesthetic duel box
        duel_box = (
            f"┌──────────────────────────────────────────────┐\n"
            f"│  {self.player1.name}: [{p1_card.display_name} · {p1_rank.name}]  ⚔️  {self.player2.name}: [{p2_card.display_name} · {p2_rank.name}] │\n"
            f"└──────────────────────────────────────────────┘"
        )
        await self.broadcast({"type": "system", "text": duel_box, "level": "card_box"})

        # Resolve winner
        winner = determine_winner(p1_card.type, p2_card.type)
        reason = get_outcome_reason(p1_card.type, p2_card.type)

        p1_delta = 0
        p2_delta = 0

        if winner == 0:
            outcome_msg = f"[SYSTEM] {reason}\n[SYSTEM] DRAW! Wagers returned (0 pts)."
        elif winner == 1:
            p1_delta = p1_rank.points
            p2_delta = -p2_rank.points
            self.player1.score += p1_delta
            self.player2.score += p2_delta
            p1_sign = "+" if p1_delta >= 0 else ""
            p2_sign = "+" if p2_delta >= 0 else ""
            outcome_msg = (
                f"[SYSTEM] {reason}\n"
                f"[SYSTEM] {self.player1.name} WINS!\n"
                f"[SYSTEM] {self.player1.name} {p1_sign}{p1_delta} | {self.player2.name} {p2_sign}{p2_delta}"
            )
        else:
            p1_delta = -p1_rank.points
            p2_delta = p2_rank.points
            self.player1.score += p1_delta
            self.player2.score += p2_delta
            p1_sign = "+" if p1_delta >= 0 else ""
            p2_sign = "+" if p2_delta >= 0 else ""
            outcome_msg = (
                f"[SYSTEM] {reason}\n"
                f"[SYSTEM] {self.player2.name} WINS!\n"
                f"[SYSTEM] {self.player2.name} {p2_sign}{p2_delta} | {self.player1.name} {p1_sign}{p1_delta}"
            )

        # Send structured reveal packet
        await self.broadcast({
            "type": "reveal",
            "p1_name": self.player1.name,
            "p2_name": self.player2.name,
            "p1_card": p1_card.type.name,
            "p1_rank": p1_rank.name,
            "p2_card": p2_card.type.name,
            "p2_rank": p2_rank.name,
            "winner": winner,
            "reason": reason,
            "outcome_text": outcome_msg,
            "p1_delta": p1_delta,
            "p2_delta": p2_delta,
            "p1_score": self.player1.score,
            "p2_score": self.player2.score
        })

        await self.broadcast({
            "type": "system",
            "text": outcome_msg,
            "level": "success" if winner > 0 else "info"
        })

        # Check deck replenishment
        p1_rep = self.player1.replenish_deck_if_empty()
        p2_rep = self.player2.replenish_deck_if_empty()
        if p1_rep:
            await self.send_to_player(self.player1.id, {
                "type": "system",
                "text": "[SYSTEM] All cards used! Deck replenished with 6 new cards (2 Rock, 2 Paper, 2 Scissors).",
                "level": "info"
            })
        if p2_rep:
            await self.send_to_player(self.player2.id, {
                "type": "system",
                "text": "[SYSTEM] All cards used! Deck replenished with 6 new cards (2 Rock, 2 Paper, 2 Scissors).",
                "level": "info"
            })

        self.round_number += 1
        self.state = STATE_WAITING_FOR_START
        self.player1.is_ready = False
        self.player2.is_ready = False

        await self.broadcast({
            "type": "system",
            "text": f"\n[SYSTEM] Type /start to begin Round {self.round_number}.",
            "level": "info"
        })

        await self.sync_state()

    async def sync_state(self):
        state_data = {
            "type": "state_sync",
            "state": self.state,
            "round": self.round_number,
            "p1_name": self.player1.name if self.player1 else "Waiting...",
            "p1_score": self.player1.score if self.player1 else 50,
            "p1_ready": self.player1.is_ready if self.player1 else False,
            "p1_locked": (self.player1.chosen_card is not None) if self.player1 else False,
            "p1_rank": self.player1.chosen_rank.name if (self.player1 and self.player1.chosen_rank) else None,
            "p2_name": self.player2.name if self.player2 else "Waiting...",
            "p2_score": self.player2.score if self.player2 else 50,
            "p2_ready": self.player2.is_ready if self.player2 else False,
            "p2_locked": (self.player2.chosen_card is not None) if self.player2 else False,
            "p2_rank": self.player2.chosen_rank.name if (self.player2 and self.player2.chosen_rank) else None,
        }
        await self.broadcast(state_data)

        # Individual player cards sync
        if self.player1 and self.player1.websocket:
            await self.send_to_player(self.player1.id, {
                "type": "cards_sync",
                "cards": [c.to_dict() for c in self.player1.cards],
                "counts": {k.value: v for k, v in self.player1.count_cards().items()}
            })
        if self.player2 and self.player2.websocket:
            await self.send_to_player(self.player2.id, {
                "type": "cards_sync",
                "cards": [c.to_dict() for c in self.player2.cards],
                "counts": {k.value: v for k, v in self.player2.count_cards().items()}
            })
