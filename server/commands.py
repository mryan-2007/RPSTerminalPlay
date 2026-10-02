import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from typing import List, Tuple
from card import CardType, WagerRank

HELP_TEXT = """
========================================
       AVAILABLE TERMINAL COMMANDS
========================================

/start
    Ready yourself for the next round.
    Both players must type /start.

/cards
    Display your remaining cards:
    [ROCK], [PAPER], [SCISSORS]

/choose <card> <rank>
    Select your card and wager rank.
    Cards: rock, paper, scissors
    Ranks:
      C  (3 points)   - Minimal risk
      B  (7 points)   - Standard wager
      A  (15 points)  - High stakes
      S  (25 points)  - Supreme gamble
    Aliases: /play <card> <rank>
    Example: /choose rock A

/clear
    Clear your visible terminal chat log.
    (Preserves game state and score)

/help
    Show this command guide.

/status
    Display current connection & round info.
========================================
"""

class CommandParser:
    def __init__(self, game):
        self.game = game

    async def handle_input(self, player_id: str, raw_text: str):
        text = raw_text.strip()
        if not text:
            return

        if text.startswith("/"):
            await self.parse_command(player_id, text)
        else:
            # Check if this input is a card or rank selection during an active round
            handled_choice = await self.game.handle_conversational_input(player_id, text)
            if not handled_choice:
                await self.handle_chat(player_id, text)

    async def handle_chat(self, player_id: str, message: str):
        player = self.game.get_player(player_id)
        if not player:
            return

        sender_role = "player1" if player == self.game.player1 else "player2"
        # Broadcast chat with sender info
        await self.game.broadcast({
            "type": "chat",
            "sender_id": player_id,
            "sender_role": sender_role,
            "sender_name": player.name,
            "text": message
        })

    async def parse_command(self, player_id: str, text: str):
        parts = text.split()
        cmd = parts[0].lower()

        if cmd == "/start":
            await self.game.set_player_ready(player_id)

        elif cmd == "/cards":
            player = self.game.get_player(player_id)
            if player:
                card_brackets = player.format_cards_bracket()
                await self.game.send_to_player(player_id, {
                    "type": "system",
                    "text": f"YOUR AVAILABLE CARDS:\n{card_brackets}",
                    "level": "cards_display"
                })

        elif cmd in ["/choose", "/play"]:
            if len(parts) == 2:
                # User typed "/choose rock" -> conversational prompt for rank
                await self.game.handle_conversational_input(player_id, parts[1])
            elif len(parts) >= 3:
                # User typed "/choose rock A"
                card_arg = parts[1]
                rank_arg = parts[2]
                await self.game.process_choice(player_id, card_arg, rank_arg)
            else:
                await self.game.send_to_player(player_id, {
                    "type": "system",
                    "text": "[SYSTEM] Usage: /choose <card> [rank] or just type the card name.\nExample: rock, then A",
                    "level": "warning"
                })

        elif cmd == "/clear":
            await self.game.send_to_player(player_id, {
                "type": "clear",
                "text": "[SYSTEM] Terminal cleared."
            })

        elif cmd == "/help":
            await self.game.send_to_player(player_id, {
                "type": "system",
                "text": HELP_TEXT,
                "level": "info"
            })

        elif cmd == "/status":
            player = self.game.get_player(player_id)
            role = "PLAYER 1 (HOST)" if player == self.game.player1 else "PLAYER 2 (CLIENT)"
            opp = self.game.get_opponent(player_id)
            opp_status = f"{opp.name} (Online)" if (opp and opp.is_connected) else "Disconnected"
            status_text = (
                f"[SYSTEM] STATUS:\n"
                f"  Role: {role}\n"
                f"  Opponent: {opp_status}\n"
                f"  State: {self.game.state}\n"
                f"  Round: {self.game.round_number}\n"
                f"  Score: P1={self.game.player1.score if self.game.player1 else 0} | "
                f"P2={self.game.player2.score if self.game.player2 else 0}"
            )
            await self.game.send_to_player(player_id, {
                "type": "system",
                "text": status_text,
                "level": "info"
            })

        else:
            await self.game.send_to_player(player_id, {
                "type": "system",
                "text": f"[SYSTEM] Unknown command '{cmd}'. Type /help for available commands.",
                "level": "danger"
            })
