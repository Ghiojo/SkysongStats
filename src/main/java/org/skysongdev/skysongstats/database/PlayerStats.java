package org.skysongdev.skysongstats.database;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.ArrayList;

public class PlayerStats {
    private String uuid;
    private String profile;
    private int strength;
    private int dexterity;
    private int constitution;
    private int focus;
    private int speed;

    private int hp;
    private int hp_max;
    private int temp_hp;
    private int ac;

    private int arcrot;
    private int arcrot_max;
    private int fictis;
    private int fictis_max;
    private int potion;
    private int potion_max;

    private ArrayList<Modifier> modifiers;

    public PlayerStats(String uuid){
        this.uuid = uuid;
        this.profile = "Default";
        this.strength = 0;
        this.dexterity = 0;
        this.constitution = 0;
        this.focus = 0;
        this.speed = 6;

        this.potion = 0;
        this.potion_max = 9;
        this.arcrot = 0;
        this.arcrot_max = 35;
        this.fictis = 0;
        this.fictis_max = 10;
        this.ac = 0;
        this.hp = 60;
        this.hp_max = 60;
        this.temp_hp = 0;

        this.modifiers = new ArrayList<Modifier>();
    }
    public PlayerStats(String uuid, String profile){
        this.uuid = uuid;
        this.profile = profile;
        this.strength = 0;
        this.dexterity = 0;
        this.constitution = 0;
        this.focus = 0;
        this.speed = 6;

        this.potion = 0;
        this.potion_max = 9;
        this.arcrot = 0;
        this.arcrot_max = 35;
        this.fictis = 0;
        this.fictis_max = 10;
        this.ac = 0;
        this.hp = 60;
        this.hp_max = 60;
        this.temp_hp = 0;

        this.modifiers = new ArrayList<Modifier>();
    }

    public PlayerStats(String uuid, String profile, int strength, int dexterity, int constitution, int focus, int speed, int ac, int potion, int arcrot, int fictis, int hp, int temp_hp, ArrayList<Modifier> modifiers){
        this.uuid = uuid;
        this.profile = profile;
        this.strength = strength;
        this.dexterity = dexterity;
        this.constitution = constitution;
        this.focus = focus;
        this.speed = speed;

        this.potion = potion;
        this.potion_max = 9 + constitution;
        this.arcrot = arcrot;
        this.arcrot_max = 35 + (2 * focus);
        this.fictis = fictis;
        this.fictis_max = 10 + focus;

        this.ac = ac;
        this.hp = hp;
        this.temp_hp = temp_hp;
        this.hp_max = 60 + (10 * constitution);

        this.modifiers = modifiers;
    }

    public void setStrength(int value){
        this.strength = value;
    }
    public void setDexterity(int value){
        this.dexterity = value;
    }
    public void setConstitution(int value){
        this.constitution = value;
        this.hp_max = 60 + (10 * value);
        this.potion_max = 9 + value;
    }
    public void setFocus(int value){
        this.focus = value;
        this.fictis_max = 10 + value;
        this.arcrot_max = 35 + (2 * value);
    }
    //Getters
    public String getUuid() {
        return uuid;
    }
    public String getProfile() {
        return profile;
    }
    public int getStrength() {
        return strength;
    }
    public int getDexterity() {
        return dexterity;
    }
    public int getConstitution() {
        return constitution;
    }
    public int getFocus() {
        return focus;
    }
    public int getSpeed() {
        return speed;
    }
    public int getHp() {
        return hp;
    }
    public int getHp_max() {
        return hp_max;
    }
    public int getTemp_hp() { return temp_hp; }
    public int getAc() {
        return ac;
    }
    public int getArcrot() {
        return arcrot;
    }
    public int getArcrot_max() {
        return arcrot_max;
    }
    public int getFictis() {
        return fictis;
    }
    public int getFictis_max() {
        return fictis_max;
    }
    public int getPotion() {
        return potion;
    }
    public int getPotion_max() {
        return potion_max;
    }
    public ArrayList<Modifier> getModifiers() { return modifiers; }
    public Modifier getModifierFromList(int index){
        try{
            return modifiers.get(index);
        } catch(IndexOutOfBoundsException e){
            Bukkit.getLogger().warning("Modifier Index is out of Bounds!");
            return null;
        }
    }
    public int getModifierIndex(Modifier mod){
        return modifiers.indexOf(mod);
    }

    //Setters
    public void setSpeed(int value){
        this.speed = value;
    }
    public void setHP(int value){
        this.hp = value;
    }
    public void setTemp_hp(int value){
        this.temp_hp = value;
    }
    public void setAC(int value){
        this.ac = value;
    }
    public void setPotion(int value){
        this.potion = value;
    }
    public void setArcrot(int value){
        this.arcrot = value;
    }
    public void setFictis(int value){
        this.fictis = value;
    }

    //Adders
    public void addStrength(int value){ setStrength(this.strength + value); }
    public void addDexterity(int value){ setDexterity(this.dexterity + value); }
    public void addConstitution(int value){ setConstitution(this.constitution + value); }
    public void addFocus(int value){ setFocus(this.focus + value); }
    public void addSpeed(int value){ setSpeed(this.speed + value); }
    public void addHP(int value) { setHP(this.hp + value); }
    public void addTemp_hp(int value) { setTemp_hp(this.temp_hp + value); }
    public void addAC(int value) { setAC(this.ac + value); }
    public void addArcrot(int value) { setArcrot(this.arcrot + value);}
    public void addPotion(int value) { setPotion(this.potion + value);}
    public void addFictis(int value) { setFictis(this.fictis + value);}
    public void addModifier(Modifier mod){
        this.modifiers.add(mod);
        if(mod.getStat() == Utils.StaticStats.MAXHP)
            if(hp > hp_max + getMaxHPModifiers())
                hp = hp_max + getMaxHPModifiers();
    }


    public int getStrengthModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.STRENGTH)
                add += m.getModifier();
        }
        return add;
    }
    public int getDexterityModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.DEXTERITY)
                add += m.getModifier();
        }
        return add;
    }
    public int getConstitutionModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.CONSTITUTION)
                add += m.getModifier();
        }
        return add;
    }
    public int getFocusModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.FOCUS)
                add += m.getModifier();
        }
        return add;
    }
    public int getSpeedModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.SPEED)
                add += m.getModifier();
        }
        return add;
    }
    public int getMaxHPModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.MAXHP)
                add += m.getModifier();
        }
        return add;
    }
    public int getMaxArcrotModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.ARCROT_MAX)
                add += m.getModifier();
        }
        return add;
    }
    public int getMaxFictisModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.FICTIS_MAX)
                add += m.getModifier();
        }
        return add;
    }
    public int getMaxPotionModifiers(){
        int add = 0;
        for(Modifier m : modifiers){
            if(m.getStat() == Utils.StaticStats.POTION_MAX)
                add += m.getModifier();
        }
        return add;
    }

    public void dealDamage(int damage){
        if(temp_hp > 0){
            temp_hp -= damage;
            if(temp_hp < 0){
                hp += temp_hp;
                temp_hp = 0;
            }
        } else {
            hp -= damage;
        }
    }
}
