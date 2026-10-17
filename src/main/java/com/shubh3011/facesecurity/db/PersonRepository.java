package com.shubh3011.facesecurity.db;

import com.shubh3011.facesecurity.model.Person;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** CRUD for {@link Person}. */
public final class PersonRepository {

    private static final String COLUMNS = "id, name, role, created_at";

    private final Database database;

    public PersonRepository(Database database) {
        this.database = database;
    }

    public Person save(Person person) {
        if (person.id() == 0L) {
            long id = database.insert(
                    "INSERT INTO person(name, role, created_at) VALUES (?, ?, ?)",
                    ps -> {
                        ps.setString(1, person.name());
                        ps.setString(2, person.role());
                        ps.setString(3, Database.toText(person.createdAt()));
                    });
            return person.withId(id);
        }
        database.update(
                "UPDATE person SET name = ?, role = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, person.name());
                    ps.setString(2, person.role());
                    ps.setLong(3, person.id());
                });
        return person;
    }

    public Optional<Person> findById(long id) {
        return database.query("SELECT " + COLUMNS + " FROM person WHERE id = ?",
                ps -> ps.setLong(1, id), PersonRepository::map).stream().findFirst();
    }

    public Optional<Person> findByName(String name) {
        return database.query("SELECT " + COLUMNS + " FROM person WHERE name = ?",
                ps -> ps.setString(1, name), PersonRepository::map).stream().findFirst();
    }

    public List<Person> findAll() {
        return database.query("SELECT " + COLUMNS + " FROM person ORDER BY name COLLATE NOCASE",
                Database.NO_PARAMETERS, PersonRepository::map);
    }

    public boolean delete(long id) {
        return database.update("DELETE FROM person WHERE id = ?", ps -> ps.setLong(1, id)) > 0;
    }

    public int count() {
        return database.query("SELECT COUNT(*) AS c FROM person", Database.NO_PARAMETERS, rs -> rs.getInt("c")).get(0);
    }

    private static Person map(ResultSet rs) throws SQLException {
        return new Person(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("role"),
                Database.toInstant(rs.getString("created_at")));
    }
}
