db.getCollection("presences.punishments").find({created_at: /(.*\/.*)/}).forEach(function (punishment) {
    punishment.created_at = punishment.created_at.replace(/\//g, "-");
    db.getCollection("presences.punishments").save(punishment);
});