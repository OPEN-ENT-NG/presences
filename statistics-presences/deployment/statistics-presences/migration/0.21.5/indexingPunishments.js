db.runCommand(
    {
        createIndexes: "presences.punishments",
        indexes: [
            {
                key: {
                    "structure_id": 1,
                    "type_id": 1,
                    "student_id": 1,
                    "fields.start_at": 1,
                    "fields.end_at": 1,
                    "created_at": 1
                },
                background: true,
                name: "presences.punishments_fields_index"
            }
        ]
    });