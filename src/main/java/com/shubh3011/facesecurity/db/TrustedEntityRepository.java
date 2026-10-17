package com.shubh3011.facesecurity.db;

import com.shubh3011.facesecurity.model.EntityKind;
import com.shubh3011.facesecurity.model.TrustedEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Stores face-gated resources. */
public final class TrustedEntityRepository {

    private static final String COLUMNS = "id, label, kind, path, person_id, created_at";

    private final Database database;

    public TrustedEntityRepository(Database database) {
        this.database = database;
    }

    public TrustedEntity save(TrustedEntity entity) {
        if (entity.id() == 0L) {
            long id = database.insert(
                    "INSERT INTO trusted_entity(label, kind, path, person_id, created_at) VALUES (?, ?, ?, ?, ?)",
                    ps -> {
                        ps.setString(1, entity.label());
                        ps.setString(2, entity.kind().name());
                        ps.setString(3, entity.path());
                        ps.setLong(4, entity.trustedPersonId());
                        ps.setString(5, Database.toText(entity.createdAt()));
                    });
            return entity.withId(id);
        }
        database.update(
                "UPDATE trusted_entity SET label = ?, kind = ?, path = ?, person_id = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, entity.label());
                    ps.setString(2, entity.kind().name());
                    ps.setString(3, entity.path());
                    ps.setLong(4, entity.trustedPersonId());
                    ps.setLong(5, entity.id());
                });
        return entity;
    }

    public Optional<TrustedEntity> findById(long id) {
        return database.query("SELECT " + COLUMNS + " FROM trusted_entity WHERE id = ?",
                ps -> ps.setLong(1, id), TrustedEntityRepository::map).stream().findFirst();
    }

    public Optional<TrustedEntity> findByPath(String path) {
        return database.query("SELECT " + COLUMNS + " FROM trusted_entity WHERE path = ?",
                ps -> ps.setString(1, path), TrustedEntityRepository::map).stream().findFirst();
    }

    public List<TrustedEntity> findAll() {
        return database.query("SELECT " + COLUMNS + " FROM trusted_entity ORDER BY label COLLATE NOCASE",
                Database.NO_PARAMETERS, TrustedEntityRepository::map);
    }

    public boolean delete(long id) {
        return database.update("DELETE FROM trusted_entity WHERE id = ?", ps -> ps.setLong(1, id)) > 0;
    }

    public int count() {
        return database.query("SELECT COUNT(*) AS c FROM trusted_entity", Database.NO_PARAMETERS, rs -> rs.getInt("c"))
                .get(0);
    }

    private static TrustedEntity map(ResultSet rs) throws SQLException {
        return new TrustedEntity(
                rs.getLong("id"),
                rs.getString("label"),
                EntityKind.valueOf(rs.getString("kind")),
                rs.getString("path"),
                rs.getLong("person_id"),
                Database.toInstant(rs.getString("created_at")));
    }
}
