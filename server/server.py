#!/usr/bin/env python3
"""
RPS Terminal Battle - Authoritative Game Server
Can run on Termux (Android), Linux, macOS, or Windows.

Installation in Termux:
  pkg update && pkg upgrade -y
  pkg install python -y
  pip install websockets
  python server.py
"""

import asyncio
import json
import socket
import sys
import os
from typing import Dict, Any

try:
    import websockets
except ImportError:
    print("[ERROR] 'websockets' library is required.")
    print("Please install it in Termux using:")
    print("    pip install websockets")
    sys.exit(1)

from game import RPSGame
from commands import CommandParser

PORT = int(os.environ.get("PORT", 8765))

def get_local_ip() -> str:
    """Finds the device's local network IP address."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Does not actually connect, just determines outbound interface
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

class ServerManager:
    def __init__(self):
        self.clients: Dict[str, Any] = {}  # player_id -> websocket
        self.ws_to_id: Dict[Any, str] = {} # websocket -> player_id
        self.game = RPSGame(self.broadcast, self.send_to_player)
        self.parser = CommandParser(self.game)

    async def broadcast(self, data: Dict[str, Any]):
        msg = json.dumps(data)
        disconnected = []
        for pid, ws in list(self.clients.items()):
            try:
                await ws.send(msg)
            except Exception:
                disconnected.append(pid)
        for pid in disconnected:
            await self.handle_client_disconnect(pid)

    async def send_to_player(self, player_id: str, data: Dict[str, Any]):
        ws = self.clients.get(player_id)
        if ws:
            try:
                await ws.send(json.dumps(data))
            except Exception:
                await self.handle_client_disconnect(player_id)

    async def handle_client_disconnect(self, player_id: str):
        ws = self.clients.pop(player_id, None)
        if ws in self.ws_to_id:
            del self.ws_to_id[ws]
        await self.game.handle_disconnect(player_id)

    async def handler(self, websocket):
        player_id = None
        try:
            # First message must be handshake: {"type": "handshake", "name": "Ryan"}
            init_msg = await asyncio.wait_for(websocket.recv(), timeout=10.0)
            data = json.loads(init_msg)
            name = data.get("name", "Operative").strip() or "Operative"
            player_id = f"p_{id(websocket)}"

            self.clients[player_id] = websocket
            self.ws_to_id[websocket] = player_id

            player, role = await self.game.add_player(player_id, name, websocket)

            # Send welcome handshake response with role assignment
            await websocket.send(json.dumps({
                "type": "handshake_ack",
                "player_id": player_id,
                "role": role,
                "name": name,
                "status": "connected"
            }))

            # Main listen loop
            async for raw_message in websocket:
                try:
                    payload = json.loads(raw_message)
                    msg_type = payload.get("type", "input")
                    if msg_type == "input":
                        text = payload.get("text", "")
                        await self.parser.handle_input(player_id, text)
                    elif msg_type == "command":
                        cmd = payload.get("command", "")
                        await self.parser.parse_command(player_id, cmd)
                    elif msg_type == "chat":
                        text = payload.get("text", "")
                        await self.parser.handle_chat(player_id, text)
                except json.JSONDecodeError:
                    # Treat plain string as terminal input
                    await self.parser.handle_input(player_id, raw_message)

        except websockets.exceptions.ConnectionClosed:
            pass
        except asyncio.TimeoutError:
            pass
        except Exception as e:
            print(f"[DEBUG] Error with client: {e}")
        finally:
            if player_id:
                await self.handle_client_disconnect(player_id)

async def main():
    manager = ServerManager()
    local_ip = get_local_ip()

    banner = f"""
============================================================
      RPS // BATTLE TERMINAL - AUTHORITATIVE SERVER
============================================================
Status: ONLINE
Listening Port : {PORT}
Host Local IP  : {local_ip}

For Friend (Player 2) to connect:
  Enter Server Address: ws://{local_ip}:{PORT}
  (Or if running client on same device: ws://127.0.0.1:{PORT})

Waiting for Player 1 (Host) and Player 2 (Client)...
============================================================
"""
    print(banner)

    async with websockets.serve(manager.handler, "0.0.0.0", PORT):
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n[SERVER] Shutdown gracefully.")
