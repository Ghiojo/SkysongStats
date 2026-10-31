package org.skysongdev.skysongstats.storage;

import com.zaxxer.hikari.HikariDataSource;

import java.util.Objects;
import java.util.function.Consumer;

// The stat, skill and proficiency tables as the plugin uses them: a connection pool, the SQL
// repositories, and the background writer every save goes through. Closing waits
// for the queued saves, then closes the pool.
public final class StatStorage implements CharacterStorage {
    private final HikariDataSource pool;
    private final AsyncWriter writer;
    private final StatRepository repository;
    private final SkillRepository skillRepository;
    private final ProficiencyRepository proficiencyRepository;

    StatStorage(HikariDataSource pool, Consumer<String> log) {
        this.pool = pool;
        this.writer = new AsyncWriter(e -> log.accept("Could not save a change: " + e.getMessage()));
        this.repository = new AsyncStatRepository(new SqlStatRepository(pool), writer);
        this.skillRepository = new AsyncSkillRepository(new SqlSkillRepository(pool), writer);
        this.proficiencyRepository = new AsyncProficiencyRepository(new SqlProficiencyRepository(pool), writer);
    }

    // log gets every save that failed. Throws when MySQL can't be reached.
    public static StatStorage open(DatabaseSettings settings, Consumer<String> log) {
        Objects.requireNonNull(log, "log");
        return new StatStorage(ConnectionPool.create(settings), log);
    }

    @Override
    public StatRepository repository() {
        return repository;
    }

    @Override
    public SkillRepository skillRepository() {
        return skillRepository;
    }

    @Override
    public ProficiencyRepository proficiencyRepository() {
        return proficiencyRepository;
    }

    @Override
    public void close() {
        writer.close();
        pool.close();
    }
}
