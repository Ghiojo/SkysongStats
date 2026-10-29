package org.skysongdev.skysongstats.database;

import org.bukkit.Bukkit;
import org.skysongdev.skysongstats.Utils.*;

import java.sql.*;
import java.util.ArrayList;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class Database {
    private final String HOST;
    private final String PORT;
    private final String USER;
    private final String PASSWORD;
    private final String DATABASE_NAME;

    public Database(String HOST, String PORT, String USER, String PASSWORD, String DATABASE_NAME){
        this.HOST = HOST;
        this.PORT = PORT;
        this.USER = USER;
        this.PASSWORD = PASSWORD;
        this.DATABASE_NAME = DATABASE_NAME;
    }

    private Connection sqlConnection;

    //Initialization
    public Connection getConnection() throws SQLException{
        if(sqlConnection != null){
            return sqlConnection;
        }
        String url = "jdbc:mysql://" + this.HOST + ":" + this.PORT + "/" + this.DATABASE_NAME;
        sqlConnection = DriverManager.getConnection(url, this.USER, this.PASSWORD);
        Bukkit.getLogger().info("Connected to the database");
        return sqlConnection;
    }
    public void initializeDatabase() throws SQLException{
        Statement buildStatement = getConnection().createStatement();
        String sqlactive = "CREATE TABLE IF NOT EXISTS skysong_active_stats(uuid varchar(36) primary key, profile varchar(20))";
        buildStatement.execute(sqlactive);
        String sqlSkills = "CREATE TABLE IF NOT EXISTS skysong_skills(id INT AUTO_INCREMENT primary key, uuid varchar(36), profile varchar(20), animal_handling INT, farming INT, forestry INT, mining INT, mist_gathering INT, wood_processing INT, alchemy INT, artificing INT, cooking INT, craftsman INT, metalworking INT, tailoring INT, witchcraft INT, economical INT, entertainment INT, martial INT, medical INT, transportation_and_exploration INT)";
        buildStatement.execute(sqlSkills);
        String sqlSetup = "CREATE TABLE IF NOT EXISTS skysong_setup(id INT AUTO_INCREMENT primary key, uuid varchar(36), profile varchar(20), setup BOOLEAN)";
        buildStatement.execute(sqlSetup);
        String sqlCharacter = "CREATE TABLE IF NOT EXISTS skysong_characters(id INT AUTO_INCREMENT primary key, uuid varchar(36), profile varchar(20), name varchar(100), age varchar(20), gender varchar(20), ancestry varchar(50), pronouns varchar(20), description varchar(500))";
        buildStatement.execute(sqlCharacter);
        String sqlDowntime = "CREATE TABLE IF NOT EXISTS skysong_downtime(id INT AUTO_INCREMENT primary key, uuid varchar(36), current_downtime INT)";
        buildStatement.execute(sqlDowntime);
        String sqlCharDowntime = "CREATE TABLE IF NOT EXISTS skysong_character_downtime(id INT AUTO_INCREMENT primary key, uuid varchar(36), profile varchar(20), current_downtime INT)";
        buildStatement.execute(sqlCharDowntime);
        String sqlDowntimePreferences = "CREATE TABLE IF NOT EXISTS skysong_downtime_preferences(uuid varchar(36) primary key, isGlobal BOOLEAN)";
        buildStatement.execute(sqlDowntimePreferences);
        buildStatement.close();
    }

    //Active Profile Handling
    public void createActiveProfileData(ProfileIndex data) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO skysong_active_stats(uuid, profile) VALUES (?, ?)");
        statement.setString(1, data.getUuid());
        statement.setString(2, data.getProfile());

        statement.executeUpdate();
        statement.close();
    }
    public void updateActiveProfileData(ProfileIndex activestat) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("UPDATE skysong_active_stats SET profile = ? WHERE uuid = ?");
        statement.setString(1, activestat.getProfile());
        statement.setString(2, activestat.getUuid());

        statement.executeUpdate();
        statement.close();
    }

    //Skill Handling
    public void createSkillsData(PlayerSkills data) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO skysong_skills(uuid, profile, animal_handling, farming, forestry, mining, mist_gathering, wood_processing, alchemy, artificing, cooking, craftsman, metalworking, tailoring, witchcraft, economical, entertainment, martial, medical, transportation_and_exploration) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        statement.setString(1, data.getUuid());
        statement.setString(2, data.getProfile());
        for (int i = 0; i < 18; i++) {
            statement.setInt(i + 3, data.getSkill(Utils.Skills.values()[i]));
        }
        statement.executeUpdate();
        statement.close();
    }
    public void updateSkillsData(PlayerSkills data) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("UPDATE skysong_skills SET animal_handling = ?, farming = ?, forestry = ?, mining = ?, mist_gathering = ?, wood_processing = ?, alchemy = ?, artificing = ?, cooking = ?, craftsman = ?, metalworking = ?, tailoring = ?, witchcraft = ?, economical = ?, entertainment = ?, martial = ?, medical = ?, transportation_and_exploration = ? WHERE uuid = ? AND profile = ?");
        for(int i = 0; i < 18; i++){
            statement.setInt(i + 1, data.getSkill(Utils.Skills.values()[i]));
        }
        statement.setString(19, data.getUuid());
        statement.setString(20, data.getProfile());
        statement.executeUpdate();
        statement.close();
    }
    public void deleteSkillData(PlayerSkills data) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("DELETE FROM skysong_skills WHERE uuid = ? AND profile = ?");
        statement.setString(1, data.getUuid());
        statement.setString(2, data.getProfile());
        statement.executeUpdate();
        statement.close();
    }

    //Setup Handling
    public void createSetupData(String uuid, String profile) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO skysong_setup(uuid, profile, setup) VALUES (?, ?, ?)");
        statement.setString(1, uuid);
        statement.setString(2, profile);
        statement.setBoolean(3, false);
        statement.executeUpdate();
        statement.close();
    }
    public void createSetupData(SetupProfile profile) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO skysong_setup(uuid, profile, setup) VALUES (?, ?, ?)");
        statement.setString(1, profile.getUuid());
        statement.setString(2, profile.getProfile());
        statement.setBoolean(3, false);
        statement.executeUpdate();
        statement.close();
    }

    public void updateSetupData(String uuid, String profile, boolean isSetUp) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("UPDATE skysong_setup SET setup = ? WHERE uuid = ? AND profile = ?");
        statement.setBoolean(1, isSetUp);
        statement.setString(2, uuid);
        statement.setString(3, profile);
        statement.executeUpdate();
        statement.close();
    }
    public void deleteSetupData(String uuid, String profile) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("DELETE FROM skysong_setup WHERE uuid = ? AND profile = ?");
        statement.setString(1, uuid);
        statement.setString(2, profile);
        statement.executeUpdate();
        statement.close();
    }

    //Character Handling
    public void createCharacterData(CharacterProfile profile) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO skysong_characters(uuid, profile, name, age, gender, ancestry, pronouns, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        statement.setString(1, profile.getUuid());
        statement.setString(2, profile.getProfile());
        statement.setString(3, profile.getName());
        statement.setString(4, profile.getAge());
        statement.setString(5, profile.getGender());
        statement.setString(6, profile.getAncestry());
        statement.setString(7, profile.getPronouns());
        statement.setString(8, profile.getDescription());
        statement.executeUpdate();
        statement.close();
    }
    public void updateCharacterData(CharacterProfile profile) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("UPDATE skysong_characters SET name = ?, age = ?, gender = ?, ancestry = ?, pronouns = ?, description = ? WHERE uuid = ? AND profile = ?");
        statement.setString(1, profile.getName());
        statement.setString(2, profile.getAge());
        statement.setString(3, profile.getGender());
        statement.setString(4, profile.getAncestry());
        statement.setString(5, profile.getPronouns());
        statement.setString(6, profile.getDescription());
        statement.setString(7, profile.getUuid());
        statement.setString(8, profile.getProfile());
        statement.executeUpdate();
        statement.close();
    }
    public void deleteCharacterData(CharacterProfile profile) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("DELETE FROM skysong_characters WHERE uuid = ? AND profile = ?");
        statement.setString(1, profile.getUuid());
        statement.setString(2, profile.getProfile());
        statement.executeUpdate();
        statement.close();
    }

    //Data Dumping
    public void dumpDatabaseData() throws SQLException{
        dumpActiveStatsDatabase();
        dumpSkillsDatabase();
        dumpSetupDatabase();
        dumpCharacterDatabase();
    }
    public void dumpActiveStatsDatabase() throws SQLException{
        ProfileManager.activeProfiles = new ArrayList<ProfileIndex>();
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * FROM skysong_active_stats";
        ResultSet results = statement.executeQuery(sql);

        while(results.next()){
            String uuid = results.getString("uuid");
            String profile = results.getString("profile");

            ProfileIndex statIndex = new ProfileIndex(uuid, profile);
            ProfileManager.activeProfiles.add(statIndex);
        }
        statement.close();
    }

    public void dumpSkillsDatabase() throws SQLException{
        SkillManager.playerSkills = new ArrayList<PlayerSkills>();
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * FROM skysong_skills";
        ResultSet results = statement.executeQuery(sql);

        while(results.next()){
            String uuid = results.getString("uuid");
            String profile = results.getString("profile");
            ArrayList<Integer> skills = new ArrayList<>();
            for(Utils.Skills s : Utils.Skills.values()){
                skills.add(results.getInt(s.toString().toLowerCase()));
            }
            PlayerSkills playerSkills = new PlayerSkills(uuid, profile, skills);
            SkillManager.playerSkills.add(playerSkills);
        }
        statement.close();
    }

    public void dumpSetupDatabase() throws SQLException{
        ProfileManager.setupProfiles = new ArrayList<SetupProfile>();
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * FROM skysong_setup";
        ResultSet results = statement.executeQuery(sql);

        while(results.next()){
            String uuid = results.getString("uuid");
            String profile = results.getString("profile");
            boolean setup = results.getBoolean("setup");

            SetupProfile setupProfile = new SetupProfile(uuid, profile, setup);
            ProfileManager.setupProfiles.add(setupProfile);
        }
        statement.close();
    }

    public void dumpCharacterDatabase() throws SQLException {
        CharacterManager.characterProfiles = new ArrayList<CharacterProfile>();
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * FROM skysong_characters";
        ResultSet results = statement.executeQuery(sql);

        while (results.next()) {
            String uuid = results.getString("uuid");
            String profile = results.getString("profile");
            String name = results.getString("name");
            String age = results.getString("age");
            String gender = results.getString("gender");
            String ancestry = results.getString("ancestry");
            String pronouns = results.getString("pronouns");
            String description = results.getString("description");

            CharacterProfile characterProfile = new CharacterProfile(uuid, profile, name, age, gender, ancestry, pronouns, description);
            getPlugin().getUtils().getCharacterManager().characterProfiles.add(characterProfile);
        }
        statement.close();
    }
    //Miscellaneous
    public boolean checkForUUID(String uuid) throws SQLException{
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * from skysong_active_stats WHERE uuid = \"" + uuid + "\"";
        ResultSet results = statement.executeQuery(sql);
        boolean isPlayer = results.next();
        statement.close();
        return isPlayer;
    }

    public void Ping() throws SQLException{
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * from skysong_active_stats";
        ResultSet results = statement.executeQuery(sql);
        statement.close();
    }
    
}
