# REAL_RESPONSES.md

Real API responses captured from a live Nagas' Treasure server with `SPRING_PROFILES_ACTIVE=dev`.

> **Screen format:** `screen[col][row]` — column-major. 5 columns, 3 rows each.
> **All field names are camelCase.**
> **Build/test:** `./gradlew test`
> **Start server:** `SPRING_PROFILES_ACTIVE=dev ./gradlew :app:bootRun`

---

# 1. Base Spin Responses

## 1.1 Normal Spin (No Cheat)
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "99900.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.311467Z",
            "currency": "USD",
            "endsSuperround": true,
            "parentId": {
                "id": "5e3143a5-1031-4ae7-81c5-228abac35f3c",
                "round": 2,
                "sessionId": "s-base"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "progressiveJackpot": {
                        "glowingRings": [
                            [
                                2,
                                0
                            ],
                            [
                                4,
                                0
                            ]
                        ],
                        "isTriggered": false
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "base",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                2,
                                2,
                                3
                            ],
                            [
                                1,
                                1,
                                2
                            ],
                            [
                                5,
                                13,
                                11
                            ],
                            [
                                3,
                                1,
                                1
                            ],
                            [
                                1,
                                1,
                                2
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "0.00",
                        "wins": []
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": true,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "0.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "5e3143a5-1031-4ae7-81c5-228abac35f3c",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "0.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-base",
                "id": "5e3143a5-1031-4ae7-81c5-228abac35f3c"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `thisMode`: "base"
- `endsSuperround`: true (if no feature triggered)
- `jackpotPools`: pool amounts per tier

---

## 1.2 FORCE_LOSS
### Cheat
```json
{
    "agentId": "agent-cap-1",
    "expiresIn": "5 minutes (or next spin)",
    "description": "Will apply on next spin: FORCE_LOSS",
    "userId": "player-cap",
    "applied": true,
    "cheat": "FORCE_LOSS"
}
```
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "99800.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.406011Z",
            "currency": "USD",
            "endsSuperround": true,
            "parentId": {
                "id": "4b3eb97f-f7e7-4ce6-a884-c7bcbddc9b30",
                "round": 2,
                "sessionId": "s-loss"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "progressiveJackpot": {
                        "glowingRings": [
                            [
                                0,
                                1
                            ]
                        ],
                        "isTriggered": false
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "base",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                1,
                                2,
                                3
                            ],
                            [
                                4,
                                5,
                                6
                            ],
                            [
                                7,
                                8,
                                1
                            ],
                            [
                                2,
                                3,
                                4
                            ],
                            [
                                5,
                                6,
                                7
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "0.00",
                        "wins": []
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": true,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "0.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "4b3eb97f-f7e7-4ce6-a884-c7bcbddc9b30",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "0.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-loss",
                "id": "4b3eb97f-f7e7-4ce6-a884-c7bcbddc9b30"
            },
            "type": null
        }
    },
    "type": "result"
}
```

---

### Key Fields
- `stages[0].totalWin`: "0.00"
- `wins`: []
- Screen: adjacent columns share zero symbols

## 1.3 FORCE_WIN_CAP
### Cheat
```json
{
    "agentId": "agent-cap-1",
    "expiresIn": "5 minutes (or next spin)",
    "description": "Will apply on next spin: FORCE_WIN_CAP",
    "userId": "player-cap",
    "applied": true,
    "cheat": "FORCE_WIN_CAP"
}
```
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "124700.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.472305Z",
            "currency": "USD",
            "endsSuperround": true,
            "parentId": {
                "id": "cf58f365-623b-4c10-ac04-2872492b398d",
                "round": 2,
                "sessionId": "s-cap"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "progressiveJackpot": {
                        "glowingRings": [
                            [
                                1,
                                1
                            ],
                            [
                                3,
                                2
                            ]
                        ],
                        "isTriggered": false
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "base",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                8,
                                8,
                                8
                            ],
                            [
                                8,
                                8,
                                8
                            ],
                            [
                                8,
                                8,
                                8
                            ],
                            [
                                8,
                                8,
                                8
                            ],
                            [
                                8,
                                8,
                                8
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "25000.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "1",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "2",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "3",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "4",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "5",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "6",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "7",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "8",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "9",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "10",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "11",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "12",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "13",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "14",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "15",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "16",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "17",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "18",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "19",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "20",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "21",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "22",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "23",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "24",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "25",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": true,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "25000.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "cf58f365-623b-4c10-ac04-2872492b398d",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "25000.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-cap",
                "id": "cf58f365-623b-4c10-ac04-2872492b398d"
            },
            "type": null
        }
    },
    "type": "result"
}
```

---

### Key Fields
- All symbol 8 (H) on screen
- 25 payline wins
- `totalWin` capped at 2000 x bet

## 1.4 FORCE_GRID
### Cheat
```json
{
    "agentId": "agent-cap-1",
    "expiresIn": "5 minutes (or next spin)",
    "description": "Will apply on next spin: FORCE_GRID",
    "userId": "player-cap",
    "applied": true,
    "cheat": "FORCE_GRID"
}
```
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "125600.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.548735Z",
            "currency": "USD",
            "endsSuperround": true,
            "parentId": {
                "id": "109e4f3d-17d4-48d5-82ba-ba0c4e108be2",
                "round": 2,
                "sessionId": "s-grid"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "base",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                1,
                                8,
                                2
                            ],
                            [
                                2,
                                8,
                                3
                            ],
                            [
                                3,
                                8,
                                4
                            ],
                            [
                                4,
                                8,
                                1
                            ],
                            [
                                1,
                                8,
                                2
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "1000.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "1",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": true,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "1000.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "109e4f3d-17d4-48d5-82ba-ba0c4e108be2",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "1000.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-grid",
                "id": "109e4f3d-17d4-48d5-82ba-ba0c4e108be2"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- Middle row all H (8): payline 1 hits 5x H = 10.0 x bet
- Screen is transposed: `screen[col][row]`

---

# 2. Free Spin Responses

## 2.1 Apply Cheat
```json
{
    "agentId": "agent-cap-1",
    "expiresIn": "5 minutes (or next spin)",
    "description": "Will apply on next spin: FORCE_FREE_SPIN",
    "userId": "player-cap",
    "applied": true,
    "cheat": "FORCE_FREE_SPIN"
}
```

---

## 2.2 Trigger Spin (base → free)
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "125840.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.644641Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "e97d44f2-6b6a-4711-b946-d7149514b8fe",
                "round": 2,
                "sessionId": "s-fs"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "freeSpins": {
                        "remain": 8,
                        "total": 8
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "free",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                2,
                                3,
                                3
                            ],
                            [
                                1,
                                9,
                                10
                            ],
                            [
                                10,
                                10,
                                9
                            ],
                            [
                                3,
                                3,
                                9
                            ],
                            [
                                13,
                                1,
                                1
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "340.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 4,
                                "payline": "9",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        0
                                    ]
                                ],
                                "symbol": 3,
                                "type": "line",
                                "win": "40.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 4,
                                "payline": "10",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        0
                                    ]
                                ],
                                "symbol": 3,
                                "type": "line",
                                "win": "40.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 3,
                                "payline": "19",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ]
                                ],
                                "symbol": 3,
                                "type": "line",
                                "win": "20.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 3,
                                "payline": "22",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ]
                                ],
                                "symbol": 3,
                                "type": "line",
                                "win": "20.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 3,
                                "payline": "24",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ]
                                ],
                                "symbol": 2,
                                "type": "line",
                                "win": "20.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 3,
                                "payline": "0",
                                "positions": [
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        2
                                    ]
                                ],
                                "symbol": 9,
                                "type": "scatter",
                                "win": "200.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "340.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "e97d44f2-6b6a-4711-b946-d7149514b8fe",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "340.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-fs",
                "id": "e97d44f2-6b6a-4711-b946-d7149514b8fe"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `thisMode`: "base"
- `nextMode`: "free"
- `features.freeSpins`: {remain: 8, total: 8}
- `endsSuperround`: false

---

## 2.3 Free Spin 1 of 8
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "134240.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.668381Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "e97d44f2-6b6a-4711-b946-d7149514b8fe",
                "round": 2,
                "sessionId": "s-fs"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "freeSpins": {
                        "remain": 7,
                        "total": 8
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "free",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                10,
                                5,
                                6
                            ],
                            [
                                10,
                                10,
                                10
                            ],
                            [
                                10,
                                10,
                                10
                            ],
                            [
                                10,
                                10,
                                10
                            ],
                            [
                                10,
                                10,
                                10
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "8400.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "1",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "3",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 6,
                                "type": "line",
                                "win": "600.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "5",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 6,
                                "type": "line",
                                "win": "600.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "6",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "7",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "9",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 6,
                                "type": "line",
                                "win": "600.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "10",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "11",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "13",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 6,
                                "type": "line",
                                "win": "600.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "15",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 6,
                                "type": "line",
                                "win": "600.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "16",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "17",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "19",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 6,
                                "type": "line",
                                "win": "600.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "21",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 6,
                                "type": "line",
                                "win": "600.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "22",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "23",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "400.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "25",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 6,
                                "type": "line",
                                "win": "600.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "8740.00"
                },
                "thisMode": "free",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "35e859e2-0fc4-4973-9210-ef2c19cc20b1",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "8740.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-fs",
                "id": "35e859e2-0fc4-4973-9210-ef2c19cc20b1"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `thisMode`: "free"
- `features.freeSpins.remain`: 7 (decremented)
- No wallet debit

---

# 3. Hold & Win Responses

## 3.1 Apply Cheat
```json
{
    "agentId": "agent-cap-1",
    "expiresIn": "5 minutes (or next spin)",
    "description": "Will apply on next spin: FORCE_HOLD_AND_WIN",
    "userId": "player-cap",
    "applied": true,
    "cheat": "FORCE_HOLD_AND_WIN"
}
```

---

## 3.2 Trigger Spin (base → holdAndWin)
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "134180.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.753766Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "91ccd2ba-05ed-482a-81e9-3b9a9029820c",
                "round": 2,
                "sessionId": "s-hw"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "holdAndWin": {
                        "respinsRemain": 3,
                        "isEnding": false,
                        "totalMultiplier": 54.0,
                        "lockedBonuses": [
                            {
                                "row": 1,
                                "col": 0,
                                "symbolId": 13,
                                "multiplier": 4.0,
                                "type": "CASH"
                            },
                            {
                                "row": 2,
                                "col": 0,
                                "symbolId": 13,
                                "multiplier": 12.0,
                                "type": "CASH"
                            },
                            {
                                "row": 2,
                                "col": 2,
                                "symbolId": 13,
                                "multiplier": 10.0,
                                "type": "CASH"
                            },
                            {
                                "row": 1,
                                "col": 3,
                                "symbolId": 13,
                                "multiplier": 10.0,
                                "type": "CASH"
                            },
                            {
                                "row": 2,
                                "col": 3,
                                "symbolId": 13,
                                "multiplier": 15.0,
                                "type": "CASH"
                            },
                            {
                                "row": 0,
                                "col": 4,
                                "symbolId": 13,
                                "multiplier": 3.0,
                                "type": "CASH"
                            }
                        ]
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "holdAndWin",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                0,
                                13,
                                13
                            ],
                            [
                                0,
                                0,
                                0
                            ],
                            [
                                0,
                                0,
                                13
                            ],
                            [
                                0,
                                13,
                                13
                            ],
                            [
                                13,
                                0,
                                0
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "40.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 3,
                                "payline": "14",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        0
                                    ]
                                ],
                                "symbol": 2,
                                "type": "line",
                                "win": "20.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 3,
                                "payline": "24",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ]
                                ],
                                "symbol": 2,
                                "type": "line",
                                "win": "20.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "40.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "91ccd2ba-05ed-482a-81e9-3b9a9029820c",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "40.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-hw",
                "id": "91ccd2ba-05ed-482a-81e9-3b9a9029820c"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `thisMode`: "base"
- `nextMode`: "holdAndWin"
- `features.holdAndWin.respinsRemain`: 3
- `features.holdAndWin.lockedBonuses`: 6+ items
- `features.holdAndWin.isEnding`: false
- `endsSuperround`: false

---

## 3.3 H&W Respin 1
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "134180.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.786574Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "91ccd2ba-05ed-482a-81e9-3b9a9029820c",
                "round": 2,
                "sessionId": "s-hw"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "holdAndWin": {
                        "respinsRemain": 3,
                        "isEnding": false,
                        "totalMultiplier": 72.0,
                        "lockedBonuses": [
                            {
                                "row": 1,
                                "col": 0,
                                "symbolId": 13,
                                "multiplier": 4.0,
                                "type": "CASH"
                            },
                            {
                                "row": 2,
                                "col": 0,
                                "symbolId": 13,
                                "multiplier": 12.0,
                                "type": "CASH"
                            },
                            {
                                "row": 2,
                                "col": 2,
                                "symbolId": 13,
                                "multiplier": 10.0,
                                "type": "CASH"
                            },
                            {
                                "row": 1,
                                "col": 3,
                                "symbolId": 13,
                                "multiplier": 10.0,
                                "type": "CASH"
                            },
                            {
                                "row": 2,
                                "col": 3,
                                "symbolId": 13,
                                "multiplier": 15.0,
                                "type": "CASH"
                            },
                            {
                                "row": 0,
                                "col": 4,
                                "symbolId": 13,
                                "multiplier": 3.0,
                                "type": "CASH"
                            },
                            {
                                "row": 0,
                                "col": 3,
                                "symbolId": 13,
                                "multiplier": 18.0,
                                "type": "CASH"
                            }
                        ]
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "holdAndWin",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                0,
                                13,
                                13
                            ],
                            [
                                0,
                                0,
                                0
                            ],
                            [
                                0,
                                0,
                                13
                            ],
                            [
                                13,
                                13,
                                13
                            ],
                            [
                                13,
                                0,
                                0
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "0.00",
                        "wins": []
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "40.00"
                },
                "thisMode": "holdAndWin",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "b0de02b2-55ef-4eaf-b0b4-193c36612cc3",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "40.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-hw",
                "id": "b0de02b2-55ef-4eaf-b0b4-193c36612cc3"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `thisMode`: "holdAndWin"
- Screen: only 0 or 11-13
- `respinsRemain`: resets to 3 if new bonus found, else decrements

---

# 4. Jackpot Responses

## 4.1 Set Jackpot Pool
```json
{
    "pools": {
        "DIAMOND": 42.0,
        "RUBY": 15.0,
        "EMERALD": 7.5,
        "SAPPHIRE": 3.0
    },
    "agentId": "agent-cap-1",
    "applied": true
}
```
### Verify
```json
{
    "data": {
        "EMERALD": 50.0,
        "SAPPHIRE": 10.0,
        "DIAMOND": 10000.0,
        "RUBY": 500.0
    }
}
```

---

## 4.2 FORCE_JACKPOT
### Cheat
```json
{
    "agentId": "agent-cap-1",
    "expiresIn": "5 minutes (or next spin)",
    "description": "Will apply on next spin: FORCE_JACKPOT",
    "userId": "player-cap",
    "applied": true,
    "cheat": "FORCE_JACKPOT"
}
```
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "134090.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:15.957177Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "2bb70c32-4633-440c-adb7-74f2663a783c",
                "round": 2,
                "sessionId": "s-jp"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "progressiveJackpot": {
                        "hitArrow": false,
                        "glowingRings": [
                            [
                                0,
                                0
                            ],
                            [
                                0,
                                1
                            ],
                            [
                                1,
                                0
                            ],
                            [
                                2,
                                0
                            ],
                            [
                                3,
                                0
                            ],
                            [
                                4,
                                0
                            ]
                        ],
                        "tier": "SAPPHIRE",
                        "win": "10.00",
                        "isTriggered": true
                    },
                    "jackpotPools": {
                        "EMERALD": 50.0,
                        "SAPPHIRE": 10.0,
                        "DIAMOND": 10000.0,
                        "RUBY": 500.0
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "base",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                2,
                                3,
                                3
                            ],
                            [
                                1,
                                1,
                                2
                            ],
                            [
                                3,
                                1,
                                1
                            ],
                            [
                                1,
                                2,
                                2
                            ],
                            [
                                3,
                                3,
                                11
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "0.00",
                        "wins": []
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "10.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "2bb70c32-4633-440c-adb7-74f2663a783c",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "10.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-jp",
                "id": "2bb70c32-4633-440c-adb7-74f2663a783c"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `features.progressiveJackpot.isTriggered`: true
- `features.progressiveJackpot.glowingRings`: 6+ positions [col,row]
- `features.progressiveJackpot.tier`: won tier
- `features.progressiveJackpot.win`: prize amount

---

## 4.3 Pool After Win
```json
{
    "data": {
        "EMERALD": 50.0,
        "SAPPHIRE": 10.0,
        "DIAMOND": 10000.0,
        "RUBY": 500.0
    }
}
```
### Key Fields
- Won tier reset to seed value

---

# 5. Nested FS → H&W Responses

## 5.1 Put Player in Free Spin Mode
```json
{
    "agentId": "agent-cap-1",
    "userId": "player-cap",
    "applied": true,
    "mode": "free"
}
```

---

## 5.2 Normal FS Spin (confirm mode)
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "134152.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:16.056731Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "071f38b6-af45-4444-a33d-5d87d1da655e",
                "round": 1,
                "sessionId": "s-nested"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "freeSpins": {
                        "remain": 7,
                        "total": 8
                    },
                    "jackpotPools": {
                        "EMERALD": 50.0,
                        "SAPPHIRE": 10.0,
                        "DIAMOND": 10000.0,
                        "RUBY": 500.0
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "free",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                8,
                                10,
                                5
                            ],
                            [
                                10,
                                10,
                                10
                            ],
                            [
                                10,
                                5,
                                13
                            ],
                            [
                                10,
                                10,
                                10
                            ],
                            [
                                10,
                                10,
                                10
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "62.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "1",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "4.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "2",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "10.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "5",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "4.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "9",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "4.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "10",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "4.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "11",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "4.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "13",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        1
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "4.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "14",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        1
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "10.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "19",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "4.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "21",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "4.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "24",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        0
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "10.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "62.00"
                },
                "thisMode": "free",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "c1d22862-9131-434e-8afe-98717c1aea24",
            "subgames": null,
            "totalBet": "1.00",
            "totalWin": "62.00",
            "transactionId": {
                "round": 1,
                "sessionId": "s-nested",
                "id": "c1d22862-9131-434e-8afe-98717c1aea24"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `thisMode`: "free"

---

## 5.3 Apply FORCE_HW_IN_FREE_SPIN
```json
{
    "agentId": "agent-cap-1",
    "expiresIn": "5 minutes (or next spin)",
    "description": "Will apply on next spin: FORCE_HW_IN_FREE_SPIN",
    "userId": "player-cap",
    "applied": true,
    "cheat": "FORCE_HW_IN_FREE_SPIN"
}
```

---

## 5.4 FS Spin Triggers H&W (Transition)
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "134154.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:16.116954Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "071f38b6-af45-4444-a33d-5d87d1da655e",
                "round": 1,
                "sessionId": "s-nested"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "freeSpins": {
                        "remain": 6,
                        "total": 8
                    },
                    "holdAndWin": {
                        "respinsRemain": 3,
                        "isEnding": false,
                        "totalMultiplier": 76.0,
                        "lockedBonuses": [
                            {
                                "row": 1,
                                "col": 2,
                                "symbolId": 13,
                                "multiplier": 20.0,
                                "type": "CASH"
                            },
                            {
                                "row": 0,
                                "col": 3,
                                "symbolId": 13,
                                "multiplier": 10.0,
                                "type": "CASH"
                            },
                            {
                                "row": 2,
                                "col": 3,
                                "symbolId": 13,
                                "multiplier": 7.0,
                                "type": "CASH"
                            },
                            {
                                "row": 0,
                                "col": 4,
                                "symbolId": 13,
                                "multiplier": 12.0,
                                "type": "CASH"
                            },
                            {
                                "row": 1,
                                "col": 4,
                                "symbolId": 13,
                                "multiplier": 20.0,
                                "type": "CASH"
                            },
                            {
                                "row": 2,
                                "col": 4,
                                "symbolId": 13,
                                "multiplier": 7.0,
                                "type": "CASH"
                            }
                        ]
                    },
                    "jackpotPools": {
                        "EMERALD": 50.0,
                        "SAPPHIRE": 10.0,
                        "DIAMOND": 10000.0,
                        "RUBY": 500.0
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "holdAndWin",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                0,
                                0,
                                0
                            ],
                            [
                                0,
                                0,
                                0
                            ],
                            [
                                0,
                                13,
                                0
                            ],
                            [
                                13,
                                0,
                                13
                            ],
                            [
                                13,
                                13,
                                13
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "2.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 4,
                                "payline": "4",
                                "positions": [
                                    [
                                        0,
                                        0
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "1.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 4,
                                "payline": "17",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        1
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        1
                                    ]
                                ],
                                "symbol": 5,
                                "type": "line",
                                "win": "1.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "64.00"
                },
                "thisMode": "free",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "13bfdfb1-708f-450b-bfa5-24d6d97c1de1",
            "subgames": null,
            "totalBet": "1.00",
            "totalWin": "64.00",
            "transactionId": {
                "round": 1,
                "sessionId": "s-nested",
                "id": "13bfdfb1-708f-450b-bfa5-24d6d97c1de1"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `thisMode`: "free"
- `nextMode`: "holdAndWin"
- `features.freeSpins.remain`: preserved (consumed 1)
- `features.holdAndWin.respinsRemain`: 3
- `features.holdAndWin.lockedBonuses`: 6+ items
- `endsSuperround`: false

---

# 6. Multi Agent Isolation

## 6.1 Set Different Pools
### Agent 1
```json
{
    "pools": {
        "DIAMOND": 99999.0
    },
    "agentId": "agent-cap-1",
    "applied": true
}
```
### Agent 2
```json
{
    "pools": {
        "DIAMOND": 10.0
    },
    "agentId": "agent-cap-2",
    "applied": true
}
```

---

## 6.2 Verify Isolation
### Agent 1 Pools
```json
{
    "data": {
        "EMERALD": 50.0,
        "SAPPHIRE": 10.0,
        "DIAMOND": 99999.0,
        "RUBY": 500.0
    }
}
```
### Agent 2 Pools
```json
{
    "data": {
        "EMERALD": 50.0,
        "SAPPHIRE": 10.0,
        "DIAMOND": 10000.0,
        "RUBY": 500.0
    }
}
```

---

## 6.3 Simultaneous Spins — Different Modes
### Agent 1 (Free Spin)
```json
{
    "data": {
        "control": {
            "balance": "136934.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:16.317985Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "8b3c02bb-c1f3-4830-b782-30fcb18bd899",
                "round": 2,
                "sessionId": "s-a1"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "freeSpins": {
                        "remain": 7,
                        "total": 8
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 99999.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "free",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                13,
                                7,
                                8
                            ],
                            [
                                7,
                                9,
                                10
                            ],
                            [
                                10,
                                5,
                                13
                            ],
                            [
                                10,
                                10,
                                10
                            ],
                            [
                                10,
                                10,
                                10
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "2600.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "6",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 7,
                                "type": "line",
                                "win": "800.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "19",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        2
                                    ]
                                ],
                                "symbol": 8,
                                "type": "line",
                                "win": "1000.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 5,
                                "payline": "22",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        2
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        2
                                    ],
                                    [
                                        4,
                                        1
                                    ]
                                ],
                                "symbol": 7,
                                "type": "line",
                                "win": "800.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "2880.00"
                },
                "thisMode": "free",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "12cea36d-894c-4a0e-b156-1eb8d198bf40",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "2880.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-a1",
                "id": "12cea36d-894c-4a0e-b156-1eb8d198bf40"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Agent 2 (Base Spin)
```json
{
    "data": {
        "control": {
            "balance": "100080.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:16.351726Z",
            "currency": "USD",
            "endsSuperround": true,
            "parentId": {
                "id": "d9419d87-1e52-4b47-8a72-20c3b3cdb4fe",
                "round": 2,
                "sessionId": "s-a2"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "progressiveJackpot": {
                        "glowingRings": [
                            [
                                0,
                                0
                            ],
                            [
                                1,
                                1
                            ],
                            [
                                4,
                                1
                            ]
                        ],
                        "isTriggered": false
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.0,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "base",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                1,
                                2,
                                2
                            ],
                            [
                                2,
                                3,
                                3
                            ],
                            [
                                10,
                                10,
                                10
                            ],
                            [
                                2,
                                3,
                                3
                            ],
                            [
                                13,
                                1,
                                1
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "180.00",
                        "wins": [
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 4,
                                "payline": "6",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ]
                                ],
                                "symbol": 2,
                                "type": "line",
                                "win": "40.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 3,
                                "payline": "11",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        1
                                    ]
                                ],
                                "symbol": 2,
                                "type": "line",
                                "win": "20.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 4,
                                "payline": "21",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        0
                                    ],
                                    [
                                        3,
                                        0
                                    ]
                                ],
                                "symbol": 2,
                                "type": "line",
                                "win": "40.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 4,
                                "payline": "23",
                                "positions": [
                                    [
                                        0,
                                        1
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        0
                                    ]
                                ],
                                "symbol": 2,
                                "type": "line",
                                "win": "40.00"
                            },
                            {
                                "matching": null,
                                "mode": null,
                                "occurs": 4,
                                "payline": "25",
                                "positions": [
                                    [
                                        0,
                                        2
                                    ],
                                    [
                                        1,
                                        0
                                    ],
                                    [
                                        2,
                                        2
                                    ],
                                    [
                                        3,
                                        0
                                    ]
                                ],
                                "symbol": 2,
                                "type": "line",
                                "win": "40.00"
                            }
                        ]
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": true,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "180.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "d9419d87-1e52-4b47-8a72-20c3b3cdb4fe",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "180.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-a2",
                "id": "d9419d87-1e52-4b47-8a72-20c3b3cdb4fe"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- Agent 1: `thisMode`: "free"
- Agent 2: `thisMode`: "base"
- Same userId, different agentId → fully isolated

---

# 7. Trial Mode

## 7.1 Trial Mode Spin
### Spin Response
```json
{
    "data": {
        "control": {
            "balance": "9999999.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:16.387669Z",
            "currency": "USD",
            "endsSuperround": true,
            "parentId": {
                "id": "4dd8b594-6c3c-4b44-b77c-5bc5e3c7e0c8",
                "round": 2,
                "sessionId": "s-trial"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "progressiveJackpot": {
                        "glowingRings": [
                            [
                                2,
                                2
                            ],
                            [
                                4,
                                2
                            ]
                        ],
                        "isTriggered": false
                    },
                    "jackpotPools": {
                        "EMERALD": 50.0,
                        "SAPPHIRE": 10.0,
                        "DIAMOND": 10000.0,
                        "RUBY": 500.0
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "base",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                3,
                                3,
                                1
                            ],
                            [
                                13,
                                1,
                                1
                            ],
                            [
                                2,
                                3,
                                3
                            ],
                            [
                                1,
                                2,
                                2
                            ],
                            [
                                1,
                                1,
                                2
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "0.00",
                        "wins": []
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": true,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "0.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "4dd8b594-6c3c-4b44-b77c-5bc5e3c7e0c8",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "0.00",
            "transactionId": {
                "round": 2,
                "sessionId": "s-trial",
                "id": "4dd8b594-6c3c-4b44-b77c-5bc5e3c7e0c8"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `balance`: "9999999.00" — fixed, never changes
- No wallet debit/credit
- No jackpot contribution

---

# 8. Init / Reconnect

## 8.1 Init — Clean Base
```json
{
    "data": {
        "control": {
            "balance": "136934.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:16.424765Z",
            "currency": "USD",
            "endsSuperround": true,
            "parentId": {
                "id": "5e98667b-ec69-4b73-aa13-648d987fec1a",
                "round": 0,
                "sessionId": "s-init"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "jackpotPools": {
                        "EMERALD": 50.0,
                        "SAPPHIRE": 10.0,
                        "DIAMOND": 10000.0,
                        "RUBY": 500.0
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "base",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                3,
                                3,
                                10
                            ],
                            [
                                8,
                                5,
                                11
                            ],
                            [
                                3,
                                5,
                                13
                            ],
                            [
                                13,
                                5,
                                10
                            ],
                            [
                                1,
                                2,
                                2
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "0.00",
                        "wins": []
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": true,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "0.00"
                },
                "thisMode": "base",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "init-de7b8216",
            "subgames": null,
            "totalBet": "1.00",
            "totalWin": "0.00",
            "transactionId": {
                "id": "init-de7b8216",
                "round": 0,
                "sessionId": "s-init"
            },
            "type": null
        }
    },
    "type": "result"
}
```

---

## 8.2 Init — Reconnect During Free Spin
```json
{
    "data": {
        "control": {
            "balance": "137074.00"
        },
        "round": {
            "bonusSpinCampaignId": null,
            "createdAt": "2026-04-04T05:18:16.502416Z",
            "currency": "USD",
            "endsSuperround": false,
            "parentId": {
                "id": "c0285e2b-7355-4a09-87a6-02c45c954081",
                "round": 2,
                "sessionId": "s-init-fs"
            },
            "promoInfo": null,
            "result": {
                "currency": null,
                "displayCoinValues": false,
                "events": null,
                "features": {
                    "freeSpins": {
                        "remain": 8,
                        "total": 8
                    },
                    "jackpotPools": {
                        "EMERALD": 51.0,
                        "SAPPHIRE": 11.7,
                        "DIAMOND": 10000.5,
                        "RUBY": 500.8
                    }
                },
                "id": null,
                "lines": 0,
                "nextMode": "free",
                "round": 0,
                "sessionId": null,
                "stages": [
                    {
                        "events": null,
                        "screen": [
                            [
                                1,
                                2,
                                2
                            ],
                            [
                                2,
                                3,
                                9
                            ],
                            [
                                2,
                                9,
                                3
                            ],
                            [
                                9,
                                1,
                                2
                            ],
                            [
                                1,
                                2,
                                2
                            ]
                        ],
                        "stage": 0,
                        "totalWin": "0.00",
                        "wins": []
                    }
                ],
                "superRound": {
                    "betSize": null,
                    "buyFeature": false,
                    "ends": false,
                    "parentId": null,
                    "roundOffset": 0,
                    "totalBet": null,
                    "totalGambleBet": null,
                    "totalGambleWin": null,
                    "totalWin": "0.00"
                },
                "thisMode": "free",
                "totalBet": null,
                "totalWin": null
            },
            "roundId": "init-09c284df",
            "subgames": null,
            "totalBet": "100.00",
            "totalWin": "0.00",
            "transactionId": {
                "id": "init-09c284df",
                "round": 2,
                "sessionId": "s-init-fs"
            },
            "type": null
        }
    },
    "type": "result"
}
```
### Key Fields
- `thisMode`: "free" — forced back into FS
- `features.freeSpins.remain`: preserved

