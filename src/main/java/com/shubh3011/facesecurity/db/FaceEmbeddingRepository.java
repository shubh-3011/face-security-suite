package com.shubh3011.facesecurity.db;

import com.shubh3011.facesecurity.model.FaceEmbedding;
import com.shubh3011.facesecurity.model.StoredFace;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Stores face embeddings as BLOBs, joined to their owning person on read so the
 * recognition engine gets name and id in a single round-trip.
 */
public final class FaceEmbeddingRepository {

    private final Database database;

    public FaceEmbeddingRepository(Database database) {
        this.database = database;
    }

    public void save(long personId, FaceEmbedding embedding, Instant createdAt) {
        byte[] blob = embedding.toBytes();
        database.insert(
                "INSERT INTO face_embedding(person_id, dimension, vector, created_at) VALUES (?, ?, ?, ?)",
                ps -> {
                    ps.setLong(1, personId);
                    ps.setInt(2, embedding.dimension());
                    ps.setBytes(3, blob);
                    ps.setString(4, Database.toText(createdAt));
                });
    }

    /** Every enrolled embedding with its owner, the input to identification. */
    public List<StoredFace> findAll() {
        return database.query(
                """
                SELECT e.person_id AS person_id, p.name AS person_name, e.vector AS vector
                FROM face_embedding e
                JOIN person p ON p.id = e.person_id
                ORDER BY e.id
                """,
                Database.NO_PARAMETERS,
                rs -> new StoredFace(
                        rs.getLong("person_id"),
                        rs.getString("person_name"),
                        FaceEmbedding.fromBytes(rs.getBytes("vector"))));
    }

    public Optional<FaceEmbedding> findFirstByPersonId(long personId) {
        return database.query(
                "SELECT vector FROM face_embedding WHERE person_id = ? ORDER BY id LIMIT 1",
                ps -> ps.setLong(1, personId),
                rs -> FaceEmbedding.fromBytes(rs.getBytes("vector"))).stream().findFirst();
    }

    public int deleteByPersonId(long personId) {
        return database.update("DELETE FROM face_embedding WHERE person_id = ?", ps -> ps.setLong(1, personId));
    }

    public int count() {
        return database.query("SELECT COUNT(*) AS c FROM face_embedding", Database.NO_PARAMETERS, rs -> rs.getInt("c"))
                .get(0);
    }
}
