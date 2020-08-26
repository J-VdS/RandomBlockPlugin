package mindurand;

import arc.*;
import arc.struct.ObjectSet;
import arc.struct.Array;
import arc.util.*;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.core.GameState;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.plugin.Plugin;
import mindustry.world.Block;


public class RandomBlockPlugin extends Plugin{
    Array<Block> blockable = new Array<>();
    ObjectSet<Block> disabled = new ObjectSet<>();
    boolean enableRandom = false;
    int numberBlocked = 20;

    //register event handlers and create variables in the constructor
    public RandomBlockPlugin(){
        //reset all block rules
        Vars.state.rules.bannedBlocks.clear();

        Events.on(ServerLoadEvent.class, ()->{
            //populate possible blocks
            final Block[] primer = {
                    //item transport
                    Blocks.conveyor,
                    Blocks.router,
                    Blocks.sorter,
                    Blocks.itemBridge,
                    //drilling
                    Blocks.mechanicalDrill,
                    Blocks.pneumaticDrill,
                    //liquid
                    Blocks.conduit,
                    Blocks.liquidJunction,
                    Blocks.liquidRouter,
                    Blocks.bridgeConduit,
                    //Power
                    Blocks.powerNode,
                    Blocks.combustionGenerator,
                    //Graphite
                    Blocks.graphitePress,
                    //sandbox
                    Blocks.powerVoid,
                    Blocks.powerSource,
                    Blocks.itemSource,
                    Blocks.itemVoid,
                    Blocks.liquidSource,
                    //scrapwalls
                    Blocks.scrapWall,
                    Blocks.scrapWallGigantic,
                    Blocks.scrapWallHuge,
                    Blocks.scrapWallLarge,
            };

            Array<String> primerArray = new Array<>();
            for(Block bp: primer){
                primerArray.add(bp.name);
            }
            for(Block b: Vars.content.blocks()){
                if(!b.isBuildable()) continue;
                if(!primerArray.contains(b.name)) {
                    blockable.add(b);
                }
            }
        });

        Events.on(PlayerJoin.class, event -> {
            //infomessage with blocked blocks
            if(enableRandom || disabled.size > 0){
                Call.onInfoMessage(event.player.con, viewBlocked());
            }
        });

        Events.on(PlayerLeave.class, event -> {
        });

        Events.on(GameOverEvent.class, event -> {
            if(disabled.size > 0 && !enableRandom){
                disabled.clear();
            }
            Vars.state.rules.bannedBlocks.clear();
        });

        Events.on(StateChangeEvent.class, event ->{
            if(event.to == GameState.State.playing && enableRandom){
                //select blocks
                disabled = selectRandom(blockable, numberBlocked);
                //block blocks
                Vars.state.rules.bannedBlocks = disabled;
                //print message to player via infomessage
                Call.onInfoMessage(viewBlocked());
            }
        });

    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("random", "[on/off/info]", "Enable/disable the RandomBlockPlugin", args -> {
           if(args.length == 0){
               enableRandom = enableRandom ^ true;
               Log.info("RandomPlugin: " + ((enableRandom)?"enabled (view banned blocks via random-blocks)":"disabled"));
           }else if(args[0].equals("on")){
               enableRandom = true;
           }else if(args[0].equals("off")){
               enableRandom = false;
           }else if(args[0].equals("info")){
               Log.info("RandomPlugin: " + ((enableRandom)?"enabled (use rules to view blockedblocks)":"disabled"));
           }else{
               Log.info("invalid command usage: random [on/off/info]");
           }
        });

        handler.register("random-blocks", "view banned blocks", args -> {
            Log.info("Banned blocks:\n"+disabled.toString());
        });
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("blocked", "Show banned blocks", (args, player) -> {
            if(enableRandom && disabled.size == 0){
                player.sendMessage("Plugin disabled...");
            }else{
                Call.onInfoMessage(player.con, viewBlocked());
            }
    });
    }

    public ObjectSet<Block> selectRandom(Array<Block> allowed, int number){
        ObjectSet<Block> ret = new ObjectSet<>();
        Array<Block> copyAllowed = allowed.copy();
        Block selected;
        while(ret.size < number){
            allowed.shuffle();
            selected = Structs.random(copyAllowed.toArray());
            ret.add(selected);
            copyAllowed.remove(selected);
        }
        return ret;
    };

    public String viewBlocked(){
        StringBuilder sb = new StringBuilder();
        sb.append("[scarlet]--- Blocked Blocks ---[]\n\n");
        disabled.forEach(b -> sb.append("\n* ").append(b.name));
        sb.append("\n\n[sky]Be creative...[]\n\nReshuffle after gameover.\n\n[green]RandomBlockPlugin[]");
        return sb.toString();
    }
}
