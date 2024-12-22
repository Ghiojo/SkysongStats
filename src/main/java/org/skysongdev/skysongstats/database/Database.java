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
        String sql = "CREATE TABLE IF NOT EXISTS skysong_stats(INT id primary key NOT NULL auto increment, uuid varchar(36), profile varchar(20), strength INT, dexterity INT, focus INT, constitution INT, speed INT, arcrot INT, potion INT, fictis INT, hp INT, temphp INT, ac INT)";
        buildStatement.execute(sql);
        String sqlactive = "CREATE TABLE IF NOT EXISTS skysong_active_stats(uuid varchar(36) primary key, profile varchar(20))";
        buildStatement.execute(sqlactive);
        String sqlmodifiers = "CREATE TABLE IF NOT EXISTS skysong_modifiers(id INT primary key NOT NULL auto increment, uuid varchar(36), profile varchar(20), index INT, stat INT, modifier INT)";
        buildStatement.execute(sqlmodifiers);
        String sqlSkills = "CREATE TABLE IF NOT EXISTS skysong_skills(id INT primary key NOT NULL auto increment, uuid varchar(36), profile varchar(20), animal INT, farming INT, forestry INT, mining INT, mist_gathering INT, wood_process INT, alchemy INT, artificing INT, cooking INT, craftsman INT, metalworking INT, tailoring INT, witchcraft INT, economical INT, scholarly INT, entertainment INT, martial INT, medical INT, tande INT)";
        buildStatement.execute(sqlSkills);
        String sqlSetup = "CREATE TABLE IF NOT EXISTS skysong_setup(id INT primary key NOT NULL auto increment, uuid varchar(36), profile varchar(20), setup BOOLEAN)";
        buildStatement.execute(sqlSetup);
        String sqlCharacter = "CREATE TABLE IF NOT EXISTS skysong_characters(id INT primary key NOT NULL auto increment, uuid varchar(36), profile varchar(20), name varchar(100), age varchar(20), gender varchar(20), ancestry varchar(50), pronouns varchar(20), description varchar(MAX))";
        buildStatement.execute(sqlCharacter);
    }

    //Stats Handling
    public void createStatData(PlayerStats data) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO skysong_stats(uuid, profile, strength, dexterity, focus, constitution, speed, arcrot, potion, fictis, hp, temphp, ac) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        statement.setString(1, data.getUuid());
        statement.setString(2, data.getProfile());
        statement.setInt(3, data.getStrength());
        statement.setInt(4, data.getDexterity());
        statement.setInt(5, data.getFocus());
        statement.setInt(6, data.getConstitution());
        statement.setInt(7, data.getSpeed());
        statement.setInt(8, data.getArcrot());
        statement.setInt(9, data.getPotion());
        statement.setInt(10, data.getFictis());
        statement.setInt(11, data.getHp());
        statement.setInt(12, data.getTemp_hp());
        statement.setInt(13, data.getAc());

        statement.executeUpdate();
        statement.close();
    }
    public void updateStatData(PlayerStats data) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("UPDATE skysong_stats SET strength = ?, dexterity = ?, focus = ?, constitution = ?, speed = ?, arcrot = ?, potion = ?, fictis = ?, hp = ?, temphp = ?, ac = ? WHERE uuid = ? AND profile = ?");

        statement.setInt(1, data.getStrength());
        statement.setInt(2, data.getDexterity());
        statement.setInt(3, data.getFocus());
        statement.setInt(4, data.getConstitution());
        statement.setInt(5, data.getSpeed());
        statement.setInt(6, data.getArcrot());
        statement.setInt(7, data.getPotion());
        statement.setInt(8, data.getFictis());
        statement.setInt(9, data.getHp());
        statement.setInt(10, data.getTemp_hp());
        statement.setInt(11, data.getAc());
        statement.setString(12, data.getUuid());
        statement.setString(13, data.getProfile());

        statement.executeUpdate();
        statement.close();
    }
    public void deleteStatData(PlayerStats data) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("DELETE FROM skysong_stats WHERE uuid = ? AND profile = ?");
        statement.setString(1, data.getUuid());
        statement.setString(2, data.getProfile());
        statement.executeUpdate();
        statement.close();
    }
    public PlayerStats findStats(String uuid, String profile) throws SQLException{
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * FROM skysong_stats WHERE uuid = \"" + uuid + "\" AND profile = \"" + profile + "\"";
        ResultSet results = statement.executeQuery(sql);
        String modsql = "SELECT * FROM skysong_modifiers WHERE uuid = \"" + uuid + "\" AND profile = \"" + profile + "\"";
        ResultSet modifiers = statement.executeQuery(modsql);

        if(results.next()){
            int strength = results.getInt("strength");
            int dexterity = results.getInt("dexterity");
            int focus = results.getInt("focus");
            int constitution = results.getInt("constitution");
            int speed = results.getInt("speed");
            int arcrot = results.getInt("arcrot");
            int potion = results.getInt("potion");
            int fictis = results.getInt("fictis");
            int ac = results.getInt("ac");
            int hp = results.getInt("hp");
            int temphp = results.getInt("temphp");
            ArrayList<Modifier> modarray = new ArrayList<Modifier>();
            while(modifiers.next()){
                modarray.add(new Modifier(Utils.toStatEnum(modifiers.getInt("stat")), modifiers.getInt("modifier")));
            }

            PlayerStats playerStats = new PlayerStats(uuid, profile, strength, dexterity, constitution, focus, speed, ac, potion, arcrot, fictis, hp, temphp, modarray);
            statement.close();

            return playerStats;
        }

        statement.close();
        return null;
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
        statement.setString(1, activestat.getUuid());
        statement.setString(2, activestat.getProfile());

        statement.executeUpdate();
        statement.close();
    }

    //Modifier Handling
    public void createModifierData(PlayerStats data, int index) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO skysong_modifiers(uuid, profile, index, stat, modifier) VALUES (?, ?, ?, ?, ?)");
        statement.setString(1, data.getUuid());
        statement.setString(2, data.getProfile());
        statement.setInt(3, index);
        statement.setInt(4, Utils.toInt(data.getModifierFromList(index).getStat()));
        statement.setInt(5, data.getModifierFromList(index).getModifier());
    }
    public void updateModifierData(PlayerStats data, int index) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("UPDATE skysong_modifiers SET stat = ?, modifier = ? WHERE uuid = ? AND profile = ? AND index = ?");
        statement.setInt(1, Utils.toInt(data.getModifierFromList(index).getStat()));
        statement.setInt(2, data.getModifierFromList(index).getModifier());
        statement.setString(3, data.getUuid());
        statement.setString(4, data.getProfile());
        statement.setInt(5, index);
        statement.executeUpdate();
    }
    public void deleteModifierData(PlayerStats data, int index) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("DELETE FROM skysong_modifiers WHERE uuid = ? AND profile = ? AND index = ?");
        statement.setString(1, data.getUuid());
        statement.setString(2, data.getProfile());
        statement.setInt(3, index);
        statement.executeUpdate();
        statement.close();
    }

    //Skill Handling
    public void createSkillsData(PlayerSkills data) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement("INSERT INTO skysong_skills(uuid, profile, animal, farming, forestry, mining, mist_gathering, wood_process, alchemy, artificing, cooking, craftsman, metalworking, tailoring, witchcraft, economical, scholarly, entertainment, martial, medical, tande) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        statement.setString(1, data.getUuid());
        statement.setString(2, data.getProfile());
        for (int i = 0; i < 19; i++) {
            statement.setInt(i + 3, data.getSkill(Utils.Skills.values()[i]));
        }
        statement.executeUpdate();
    }
    public void updateSkillsData(PlayerSkills data) throws SQLException{
        PreparedStatement statement = getConnection().prepareStatement("UPDATE skysong_skills SET animal = ?, farming = ?, forestry = ?, mining = ?, mist_gathering = ?, wood_process = ?, alchemy = ?, artificing = ?, cooking = ?, craftsman = ?, metalworking = ?, tailoring = ?, witchcraft = ?, economical = ?, scholarly = ?, entertainment = ?, martial = ?, medical = ?, tande = ? WHERE uuid = ? AND profile = ?");
        for(int i = 0; i < 19; i++){
            statement.setInt(i + 1, data.getSkill(Utils.Skills.values()[i]));
        }
        statement.setString(20, data.getUuid());
        statement.setString(21, data.getProfile());
        statement.executeUpdate();
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
        dumpStatsDatabase();
        dumpActiveStatsDatabase();
        dumpSkillsDatabase();
        dumpSetupDatabase();
        dumpModifiersDatabase();
        dumpCharacterDatabase();
    }
    public void dumpStatsDatabase() throws SQLException{
        StatsManager.statsProfileList = new ArrayList<PlayerStats>();
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * FROM skysong_stats";
        ResultSet results = statement.executeQuery(sql);
        ArrayList<Modifier> modarray = new ArrayList<Modifier>();

        while(results.next()){
            String uuid = results.getString("uuid");
            String profile = results.getString("profile");
            int strength = results.getInt("strength");
            int dexterity = results.getInt("dexterity");
            int constitution = results.getInt("constitution");
            int focus = results.getInt("focus");
            int speed = results.getInt("speed");
            int potion = results.getInt("potion");
            int arcrot = results.getInt("arcrot");
            int fictis = results.getInt("fictis");
            int hp = results.getInt("hp");
            int temp_hp = results.getInt("temphp");
            int ac = results.getInt("ac");
            String modsql = "SELECT * FROM skysong_modifiers WHERE uuid = \"" + uuid + "\" AND profile = \"" + profile + "\"";
            ResultSet modifiers = statement.executeQuery(modsql);

            while(modifiers.next()){
                modarray.add(new Modifier(Utils.toStatEnum(modifiers.getInt("stat")), modifiers.getInt("modifier")));
            }

            PlayerStats playerStats = new PlayerStats(uuid, profile, strength, dexterity, constitution, focus, speed, ac, potion, arcrot, fictis, hp, temp_hp, modarray);
            StatsManager.statsProfileList.add(playerStats);
        }
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
    }

    public void dumpModifiersDatabase() throws SQLException{
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * FROM skysong_modifiers";
        ResultSet results = statement.executeQuery(sql);

        while(results.next()){
            String uuid = results.getString("uuid");
            String profile = results.getString("profile");
            int index = results.getInt("index");
            Utils.StaticStats stat = Utils.toStatEnum(results.getInt("stat"));
            int modifier = results.getInt("modifier");

            Modifier mod = new Modifier(stat, modifier);
            getPlugin().getUtils().getStatsManager().findStats(uuid, profile).addModifier(mod);
        }
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
    }

    //Miscellaneous
    public boolean checkForUUID(String uuid) throws SQLException{
        Statement statement = getConnection().createStatement();
        String sql = "SELECT * from skysong_stats WHERE uuid = \"" + uuid + "\"";
        ResultSet results = statement.executeQuery(sql);
        return results.next();
    }
    
}
