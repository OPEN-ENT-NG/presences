db.runCommand(
    {
        createIndexes: "presences.statistics",
        indexes: [
            {
                key: {
                    "indicator": 1,
                    "structure": 1,
                    "user": 1
                },
                background: true,
                name: "presences.statistics_delete_old_values_index"
            }
        ]
    });