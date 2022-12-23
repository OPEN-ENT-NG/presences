db.getCollection("presences.statistics").dropIndexes();
db.runCommand(
    {
        createIndexes: "presences.statistics",
        indexes: [
            {
                key: {
                    "structure": 1,
                    "type": 1,
                    "start_date": 1,
                    "end_date": 1,
                },
                background: true,
                name: "presences.statistics_fields_index"
            },
            {
                key: {
                    "reason": 1
                },
                background: true,
                name: "presences.statistics_reason_index"
            },
            {
                key: {
                    "punishment_type": 1
                },
                background: true,
                name: "presences.statistics_punishment_type_index"
            },
            {
                key: {
                    "audiences": 1
                },
                background: true,
                name: "presences.statistics_audiences_index"
            },
            {
                key: {
                    "user": 1
                },
                background: true,
                name: "presences.statistics_user_index"
            },
        ]
    });