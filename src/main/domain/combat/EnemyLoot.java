package main.domain.combat;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
public class EnemyLoot {
    private final double statDropChance;
    private final int minStatDrop;
    private final int maxStatDrop;
    private final double itemDropChance;
    private final String itemId;
    
    public EnemyLoot(double statDropChance,int minStatDrop,int maxStatDrop,double itemDropChance,String itemId){
        this.statDropChance=statDropChance;
        this.maxStatDrop=maxStatDrop;
        this.minStatDrop=minStatDrop;
        this.itemDropChance=itemDropChance;
        this.itemId=itemId;
    }
    
    public double getStatDropChance(){
        return statDropChance;
    }
    
    public int getMaxStatDrop(){
        return maxStatDrop;
    }
    
    public int getMinStatDrop(){
        return minStatDrop;
    }
    
    public double getItemDropChance(){
        return itemDropChance;
    }
    
    public String getItemId(){
        return itemId;
    }
    
    public boolean dropItem(){
        return itemId!=null;
    }
}
