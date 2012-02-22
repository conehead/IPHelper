IPHelper
========

This is a plugin meant to simplify IP tracking, banning, and unbanning. Alternate accounts can be a pain.

Setting up IPHelper
-------------------

There are two permission nodes for IPHelper:

    ip.lookup: | Allows a player to look up a player/IP
    ip.ban:    | Allows a player to ban/unban a player/IP
There's also the `ip.*` node that allows a player to do everything.

Usage
-----

The syntax for the command is as follows:

    /ip <lookup|ban|unban> <(user/player/name)|ip> NameOrIP

For example, if I wanted to look up my own IP address and other accounts that use his/her IP address(es), I would do `/ip lookup name ConnorJames`. If I wanted to ban that user, I would do `/ip ban name ConnorJames`. "Name" is interchangeable with "player" or "user". If I wanted to ban an IP, I'd do `/ip ban ip 127.0.0.1`.