"""
Shared protocol definitions for RPS Terminal Battle.
Used for message serialization and command parsing.
"""

# Message Types
MSG_CHAT = "chat"
MSG_SYSTEM = "system"
MSG_COMMAND = "command"
MSG_STATE_SYNC = "state_sync"
MSG_REVEAL = "reveal"
MSG_CARDS = "cards"
MSG_ERROR = "error"
MSG_HANDSHAKE = "handshake"
MSG_CLEAR = "clear"

# Game States
STATE_WAITING_FOR_PLAYERS = "WAITING_FOR_PLAYERS"
STATE_CONNECTED = "CONNECTED"
STATE_WAITING_FOR_START = "WAITING_FOR_START"
STATE_ROUND_START = "ROUND_START"
STATE_WAITING_FOR_CHOICES = "WAITING_FOR_CHOICES"
STATE_BOTH_CHOICES_LOCKED = "BOTH_CHOICES_LOCKED"
STATE_REVEAL = "REVEAL"
STATE_RESOLVE = "RESOLVE"
STATE_UPDATE_SCORE = "UPDATE_SCORE"
STATE_NEXT_ROUND = "NEXT_ROUND"

# Rank point values
RANK_POINTS = {
    "C": 3,
    "B": 7,
    "A": 15,
    "S": 25
}

RANK_COLORS = {
    "C": "#FFFFFF",  # White/Gray
    "B": "#00E5FF",  # Cyan
    "A": "#FFD700",  # Yellow/Gold
    "S": "#FF0055"   # Magenta/Red
}
