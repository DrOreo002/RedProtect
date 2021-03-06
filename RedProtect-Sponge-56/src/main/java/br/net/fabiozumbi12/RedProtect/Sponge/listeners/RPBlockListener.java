package br.net.fabiozumbi12.RedProtect.Sponge.listeners;

import br.net.fabiozumbi12.RedProtect.Sponge.*;
import br.net.fabiozumbi12.RedProtect.Sponge.actions.EncompassRegionBuilder;
import br.net.fabiozumbi12.RedProtect.Sponge.config.RPLang;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntityTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.explosive.fireball.Fireball;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.entity.weather.Lightning;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.LightningEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.IgniteEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.List;

public class RPBlockListener {
	
	private static final RPContainer cont = new RPContainer();
	
	public RPBlockListener(){
		RedProtect.get().logger.debug(LogLevel.BLOCKS,"Loaded RPBlockListener...");
	}    
    
	@Listener(order = Order.FIRST, beforeModifications = true)
    public void onSignPlace(ChangeSignEvent e, @First Player p) {   
    	RedProtect.get().logger.debug(LogLevel.BLOCKS,"BlockListener - Is SignChangeEvent event! Cancelled? " + e.isCancelled());
    	
    	Sign s = e.getTargetTile();
    	List<Text> lines = e.getText().asList();
        Location<World> loc = s.getLocation();
        World w = p.getWorld();
        BlockSnapshot b = loc.createSnapshot();

        Region signr = RedProtect.get().rm.getTopRegion(loc, this.getClass().getName());
                
        if (signr != null && !signr.canSign(p)){
        	RPLang.sendMessage(p, "playerlistener.region.cantinteract");
        	e.setCancelled(true);
        	return;
        }
        
        Text line1 = lines.get(0);
        
        if (lines.size() != 4) {
            this.setErrorSign(e, p, RPLang.get("blocklistener.sign.wronglines"));
            return;
        }
        
        if (RedProtect.get().cfgs.root().server_protection.sign_spy.enabled){
        	Sponge.getServer().getConsole().sendMessage(RPUtil.toText(RPLang.get("blocklistener.signspy.location").replace("{x}", ""+loc.getX()).replace("{y}", ""+loc.getY()).replace("{z}", ""+loc.getZ()).replace("{world}", w.getName())));
        	Sponge.getServer().getConsole().sendMessage(RPUtil.toText(RPLang.get("blocklistener.signspy.player").replace("{player}", p.getName())));
        	Sponge.getServer().getConsole().sendMessage(RPUtil.toText(RPLang.get("blocklistener.signspy.lines12").replace("{line1}", lines.get(0).toPlain()).replace("{line2}", lines.get(1).toPlain())));
        	Sponge.getServer().getConsole().sendMessage(RPUtil.toText(RPLang.get("blocklistener.signspy.lines34").replace("{line3}", lines.get(2).toPlain()).replace("{line4}", lines.get(3).toPlain())));
        	if (!RedProtect.get().cfgs.root().server_protection.sign_spy.only_console){
        		for (Player play:Sponge.getServer().getOnlinePlayers()){
        			if (RedProtect.get().ph.hasGenPerm(play, "redprotect.signspy")/* && !play.equals(p)*/){
        				play.sendMessage(RPUtil.toText(RPLang.get("blocklistener.signspy.location").replace("{x}", ""+loc.getX()).replace("{y}", ""+loc.getY()).replace("{z}", ""+loc.getZ()).replace("{world}", w.getName())));
        	        	play.sendMessage(RPUtil.toText(RPLang.get("blocklistener.signspy.player").replace("{player}", p.getName())));
        	        	play.sendMessage(RPUtil.toText(RPLang.get("blocklistener.signspy.lines12").replace("{line1}", lines.get(0).toPlain()).replace("{line2}", lines.get(1).toPlain())));
        	        	play.sendMessage(RPUtil.toText(RPLang.get("blocklistener.signspy.lines34").replace("{line3}", lines.get(2).toPlain()).replace("{line4}", lines.get(3).toPlain())));
        			}
        		}
        	}
        }
        
        if ((RedProtect.get().cfgs.root().private_cat.use && s.getType().equals(TileEntityTypes.SIGN))) {
        	Region r = RedProtect.get().rm.getTopRegion(loc, this.getClass().getName());
        	Boolean out = RedProtect.get().cfgs.root().private_cat.allow_outside;
        	//private sign
        	if (cont.validatePrivateSign(lines.get(0).toPlain())){
				if (out || r != null){
					if (cont.isContainer(b)){
						int length = p.getName().length();
						if (length > 16) {
							length = 16;
						}
						lines.set(1, RPUtil.toText(p.getName().substring(0, length)));
						e.getText().setElements(lines);
						RPLang.sendMessage(p, "blocklistener.container.protected");
						return;
					} else {
						RPLang.sendMessage(p, "blocklistener.container.notprotected");
						//RedProtect.get().getPVHelper().digBlock(p, ItemStack.of(ItemTypes.SIGN,1), s.getLocation().getBlockPosition());
						return;
					}
				} else {
					RPLang.sendMessage(p, "blocklistener.container.notregion");
					//RedProtect.get().getPVHelper().digBlock(p, ItemStack.of(ItemTypes.SIGN,1), s.getLocation().getBlockPosition());
					return;
				}
			}
        }
                
        if (line1.toPlain().equalsIgnoreCase("[rp]")){
        	String claimmode = RedProtect.get().cfgs.getWorldClaimType(p.getWorld().getName());
            if ((!claimmode.equalsIgnoreCase("BLOCK") && !claimmode.equalsIgnoreCase("BOTH")) && !RedProtect.get().ph.hasPerm(p, "redprotect.admin.create")) {
                this.setErrorSign(e, p, RPLang.get("blocklistener.region.claimmode"));
                return;
            }
                    	
        	RegionBuilder rb = new EncompassRegionBuilder(e);
        	if (rb.ready()) {
                Region r = rb.build();
                lines.set(0, RPUtil.toText(RPLang.get("blocklistener.region.signcreated")));
                lines.set(1, RPUtil.toText(r.getName()));
                e.getText().setElements(lines);
                //RPLang.sendMessage(p, RPLang.get("blocklistener.region.created").replace("{region}",  r.getName()));                
                RedProtect.get().rm.add(r, RedProtect.get().serv.getWorld(r.getWorld()).get());
            }
        }
    }
    
    void setErrorSign(ChangeSignEvent e, Player p, String error) {
        List<Text> lines = e.getTargetTile().get(Keys.SIGN_LINES).get();
        lines.set(0, RPUtil.toText(RPLang.get("regionbuilder.signerror")));
        e.getTargetTile().offer(Keys.SIGN_LINES, lines);
        RPLang.sendMessage(p, RPLang.get("regionbuilder.signerror") + ": " + error);
    }
    
	@Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockPlace(ChangeBlockEvent.Place e, @First Player p) {
    	RedProtect.get().logger.debug(LogLevel.BLOCKS,"BlockListener - Is BlockPlaceEvent event! Cancelled? " + e.isCancelled());
    	    	
    	BlockSnapshot b = e.getTransactions().get(0).getOriginal();
    	Location<World> bloc = b.getLocation().get();
    	World w = bloc.getExtent();
    	
        ItemType m = RedProtect.get().getPVHelper().getItemInHand(p);
        Boolean antih = RedProtect.get().cfgs.root().region_settings.anti_hopper;
        Region r = RedProtect.get().rm.getTopRegion(b.getLocation().get(), this.getClass().getName());
        
        if (r == null && RedProtect.get().cfgs.gFlags().worlds.get(w.getName()).if_build_false.place_blocks.contains(b.getState().getType().getName())){
        	return;
        }
        
        if (r != null){
        	
        	if (!r.canMinecart(p) && m.getName().contains("minecart")){
        		RPLang.sendMessage(p, "blocklistener.region.cantplace");
                e.setCancelled(true);
            	return;
            }
        	
        	if (b.getState().getType().equals(BlockTypes.MOB_SPAWNER) && r.allowSpawner(p)){
        		return;
        	}
        	
        	try {
                if (!r.canBuild(p) && !r.canPlace(b)) {
                	RPLang.sendMessage(p, "blocklistener.region.cantbuild");
                    e.setCancelled(true);
                } else {
                	if (!RedProtect.get().ph.hasPerm(p, "redprotect.bypass") && antih && 
                			(m.equals(ItemTypes.HOPPER) || m.getName().contains("rail"))){
                		int x = bloc.getBlockX();
                		int y = bloc.getBlockY();
                		int z = bloc.getBlockZ();
                		BlockSnapshot ib = w.createSnapshot(x, y+1, z);
                		if (!cont.canBreak(p, ib) || !cont.canBreak(p, b)){
                			RPLang.sendMessage(p, "blocklistener.container.chestinside");
                			e.setCancelled(true);
                        }
                	}
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } 
        }    
    }    	
    
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockBreak(ChangeBlockEvent.Break e, @First Player p) {
    	RedProtect.get().logger.debug(LogLevel.BLOCKS,"BlockListener - Is ChangeBlockEvent.Break event!");
    	
    	BlockSnapshot b = e.getTransactions().get(0).getOriginal();
    	Location<World> bloc = b.getLocation().get();

        Boolean antih = RedProtect.get().cfgs.root().region_settings.anti_hopper;
        Region r = RedProtect.get().rm.getTopRegion(bloc, this.getClass().getName());
        
        if (!RedProtect.get().ph.hasPerm(p, "redprotect.bypass")){
			BlockSnapshot ib = bloc.getBlockRelative(Direction.UP).createSnapshot();
    		if ((antih && !cont.canBreak(p, ib)) || !cont.canBreak(p, b)){
    			RPLang.sendMessage(p, "blocklistener.container.breakinside");
    			e.setCancelled(true);
    			return;
    		}
        }
        
        if (r == null && RedProtect.get().cfgs.gFlags().worlds.get(p.getWorld().getName()).if_build_false.break_blocks.contains(b.getState().getType().getName())){
        	return;
        }
        
        if (r != null && b.getState().getType().equals(BlockTypes.MOB_SPAWNER) && r.allowSpawner(p)){
    		return;
    	}
        
        if (r != null && !r.canBuild(p) && !r.canTree(b) && !r.canMining(b) && !r.canCrops(b) && !r.canBreak(b)){
        	RPLang.sendMessage(p, "blocklistener.region.cantbuild");
            e.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onFireSpread(ChangeBlockEvent.Place e, @First LocatableBlock locatable) {
		RedProtect.get().logger.debug(LogLevel.BLOCKS,"BlockListener - Is onFireSpread event!");

		BlockState sourceState = locatable.getBlockState();

    	if (sourceState.getType() == BlockTypes.FIRE || sourceState.getType() == BlockTypes.LAVA || sourceState.getType() == BlockTypes.FLOWING_LAVA){
    		Region r = RedProtect.get().rm.getTopRegion(e.getTransactions().get(0).getOriginal().getLocation().get(), this.getClass().getName());
    		if (r != null && !r.canFire()){
				RedProtect.get().logger.debug(LogLevel.BLOCKS, "Tryed to PLACE FIRE!");
    			e.setCancelled(true);
    		}
    	}
    }

	@Listener(order = Order.FIRST, beforeModifications = true)
	public void onFireSpread(ChangeBlockEvent.Break e, @First LocatableBlock locatable) {
		RedProtect.get().logger.debug(LogLevel.BLOCKS,"RPBlockListener - Is onBlockBreakGeneric event");

		BlockState sourceState = locatable.getBlockState();

		if (sourceState.getType() == BlockTypes.FIRE){
			BlockSnapshot b = e.getTransactions().get(0).getOriginal();
			Region r = RedProtect.get().rm.getTopRegion(b.getLocation().get(), this.getClass().getName());
			if (r != null && !r.canFire() && b.getState().getType() != BlockTypes.FIRE){
				RedProtect.get().logger.debug(LogLevel.BLOCKS, "Tryed to break from FIRE!");
				e.setCancelled(true);
			}
		}
	}

	@Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockGrow(ChangeBlockEvent.Grow e) {
    	RedProtect.get().logger.debug(LogLevel.BLOCKS,"RPBlockListener - Is ChangeBlockEvent.Grow event");
		
    	BlockSnapshot b = e.getTransactions().get(0).getOriginal();
		Region r = RedProtect.get().rm.getTopRegion(b.getLocation().get(), this.getClass().getName());
		if (r != null && !r.canGrow()){
			e.setCancelled(true);
			RedProtect.get().logger.debug(LogLevel.BLOCKS, "Cancel grow "+b.getState().getName());
		}
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onFrameAndBoatBrake(DamageEntityEvent e) {
    	RedProtect.get().logger.debug(LogLevel.BLOCKS,"Is BlockListener - DamageEntityEvent event");

    	Entity ent = e.getTargetEntity();
    	Location<World> l = e.getTargetEntity().getLocation();
    	    
    	Region r = RedProtect.get().rm.getTopRegion(l, this.getClass().getName());
		if (r == null) return;

    	if (ent instanceof Hanging && e.getCause().first(Monster.class).isPresent()) {    		
    		if (!r.canFire()){
    			e.setCancelled(true);
        		return;
    		}
        }   
    	
    	if ((ent instanceof Boat || ent instanceof Minecart)  && e.getCause().first(Player.class).isPresent()){
    		Player p = e.getCause().first(Player.class).get();
    		if (!r.canMinecart(p)){
    			RPLang.sendMessage(p, "blocklistener.region.cantbreak");
    			e.setCancelled(true);
            }
    	}
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockStartBurn(IgniteEntityEvent e){
    	
    	Entity b = e.getTargetEntity();
    	Cause ignit = e.getCause(); 

    	RedProtect.get().logger.debug(LogLevel.BLOCKS,"Is BlockIgniteEvent event. Canceled? " + e.isCancelled());
    	
    	Region r = RedProtect.get().rm.getTopRegion(b.getLocation(), this.getClass().getName());
		if (r != null && !r.canFire()){
			if (ignit.first(Player.class).isPresent()){
				Player p = ignit.first(Player.class).get();
				if (!r.canBuild(p)){
					RPLang.sendMessage(p, "blocklistener.region.cantplace");
					e.setCancelled(true);
					return;
				}
			} else {
				e.setCancelled(true);
	    		return;
			}
			
			if (ignit.first(BlockSnapshot.class).isPresent() && (ignit.first(BlockSnapshot.class).get().getState().getType().equals(BlockTypes.FIRE) || ignit.first(BlockSnapshot.class).get().getState().getType().getName().contains("lava"))){
				e.setCancelled(true);
	    		return;
			} 
			if (ignit.first(Lightning.class).isPresent() || ignit.first(Explosion.class).isPresent() || ignit.first(Fireball.class).isPresent()){
				e.setCancelled(true);
            }
		}
    }

	@Listener(order = Order.FIRST, beforeModifications = true)
	public void onFlow(ChangeBlockEvent.Pre e, @First LocatableBlock locatable){
		RedProtect.get().logger.debug(LogLevel.BLOCKS,"Is BlockListener - onFlow event");

		BlockState sourceState = locatable.getBlockState();

		//liquid check
		MatterProperty mat = sourceState.getProperty(MatterProperty.class).orElse(null);
		if (mat != null && mat.getValue() == MatterProperty.Matter.LIQUID){
			e.getLocations().forEach(loc ->{
				Region r = RedProtect.get().rm.getTopRegion(loc, this.getClass().getName());
				if (r != null && !r.canFlow()){
					e.setCancelled(true);
				}
			});
		}
	}

	@Listener(order = Order.FIRST, beforeModifications = true)
	public void onLightning(LightningEvent.Pre e, @First Lightning light){
		RedProtect.get().logger.debug(LogLevel.BLOCKS,"Is LightningStrikeEvent event");
		Location<World> l = light.getLocation();
		Region r = RedProtect.get().rm.getTopRegion(l, this.getClass().getName());
		if (r != null && !r.canFire()){
			e.setCancelled(true);
        }
	}

	@Listener(order = Order.FIRST, beforeModifications = true)
	public void onDecay(ChangeBlockEvent.Decay e){
		BlockSnapshot bfrom = e.getTransactions().get(0).getOriginal();
		RedProtect.get().logger.debug(LogLevel.BLOCKS,"Is BlockFromToEvent.Decay event is to " + bfrom.getState().getType().getName() + " from " + bfrom.getState().getType().getName());
		Region r = RedProtect.get().rm.getTopRegion(bfrom.getLocation().get(), this.getClass().getName());
		if (r != null && !r.leavesDecay()){
			e.setCancelled(true);
		}
	}

	@Listener(order = Order.FIRST, beforeModifications = true)
    public void onInteractBlock(InteractBlockEvent event, @First Player p) {
    	BlockSnapshot b = event.getTargetBlock();
        Location<World> l;
        
        RedProtect.get().logger.debug(LogLevel.PLAYER,"RPBlockListener - Is InteractBlockEvent event");
        
        if (!b.getState().getType().equals(BlockTypes.AIR)){
        	l = b.getLocation().get();
        	RedProtect.get().logger.debug(LogLevel.PLAYER,"RPBlockListener - Is InteractBlockEvent event. The block is " + b.getState().getType().getName());
        } else {
        	l = p.getLocation();
        }
        
        Region r = RedProtect.get().rm.getTopRegion(l, this.getClass().getName());
        if (r != null){
        	ItemType itemInHand = RedProtect.get().getPVHelper().getItemInHand(p);
            if (itemInHand.equals(ItemTypes.ARMOR_STAND) && !r.canBuild(p)){
    			RPLang.sendMessage(p, "blocklistener.region.cantbuild");
                event.setCancelled(true); 
    		}
        }        
    }
    
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onInteractPrimBlock(InteractBlockEvent.Primary event, @First Player p) {
    	BlockSnapshot b = event.getTargetBlock();
        
        RedProtect.get().logger.debug(LogLevel.PLAYER,"RPBlockListener - Is InteractBlockEvent.Primary event");
        
    	if (!RedProtect.get().ph.hasPerm(p, "redprotect.bypass")){        	
    		if (b.getState().getType().getName().contains("sign") && !cont.canBreak(p, b)){
    			RPLang.sendMessage(p, "blocklistener.container.breakinside");
    			event.setCancelled(true);
            }
        }
    }
}
