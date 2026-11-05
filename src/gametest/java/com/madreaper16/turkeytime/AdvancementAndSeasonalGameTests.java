package com.madreaper16.turkeytime;

import com.madreaper16.turkeytime.config.TurkeyConfig;
import com.madreaper16.turkeytime.entity.TurkeyEggProjectile;
import com.madreaper16.turkeytime.event.SeasonalEvent;
import com.madreaper16.turkeytime.registry.TurkeyItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.time.LocalDate;
import java.util.List;

public final class AdvancementAndSeasonalGameTests {
    private static ServerPlayer player(GameTestHelper test) {
        return (ServerPlayer) test.makeMockServerPlayer(GameType.SURVIVAL);
    }
    private static boolean done(GameTestHelper test, ServerPlayer player, String id) {
        var advancement = test.getLevel().getServer().getAdvancements().get(TurkeyTime.id(id));
        test.assertTrue(advancement != null, "Advancement loads: " + id);
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
    private static void floor(GameTestHelper test) {
        for (int x=0;x<8;x++) for (int z=0;z<8;z++) test.setBlock(x,0,z,Blocks.STONE);
    }

    @GameTest(padding=16)
    public void hunterExcludesPoultsAndOtherAnimals(GameTestHelper test) {
        floor(test);
        var player=player(test);
        var baby=test.spawn(TurkeyTime.TURKEY,2,1,2); baby.setBaby(true);
        baby.hurtServer(test.getLevel(),test.getLevel().damageSources().playerAttack(player),100);
        var chicken=test.spawn(EntityTypes.CHICKEN,3,1,3);
        chicken.hurtServer(test.getLevel(),test.getLevel().damageSources().playerAttack(player),100);
        test.assertTrue(!done(test,player,"turkey_hunter"),"Poults and chickens do not count");
        var hen=test.spawn(TurkeyTime.TURKEY,4,1,3); hen.setMale(false); hen.setBaby(false);
        hen.hurtServer(test.getLevel(),test.getLevel().damageSources().playerAttack(player),100);
        test.assertTrue(done(test,player,"turkey_hunter"),"Adult hen counts");
        var other=player(test); var tom=test.spawn(TurkeyTime.TURKEY,5,1,3); tom.setMale(true); tom.setBaby(false);
        tom.hurtServer(test.getLevel(),test.getLevel().damageSources().playerAttack(other),100);
        test.assertTrue(done(test,other,"turkey_hunter"),"Adult tom counts");
        test.succeed();
    }

    @GameTest(padding=16)
    public void breedingAndFeastPlacementCreditActualActions(GameTestHelper test) {
        floor(test); var player=player(test);
        player.getInventory().add(new ItemStack(TurkeyItems.THANKSGIVING_FEAST));
        test.assertTrue(!done(test,player,"happy_thanksgiving"),"Possession does not count as placement");
        var a=test.spawn(TurkeyTime.TURKEY,2,1,2); var b=test.spawn(TurkeyTime.TURKEY,3,1,2);
        a.setBaby(false); b.setBaby(false); a.setInLove(player); b.setInLove(player);
        a.spawnChildFromBreeding(test.getLevel(),b);
        test.assertTrue(done(test,player,"turkey_breeder"),"Successful breeding counts before hatching parent advancement");
        test.assertTrue(!done(test,player,"turkey_hatchery"),"Breeding never grants egg hatching");
        var pos=test.absolutePos(new BlockPos(4,0,4));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TurkeyItems.THANKSGIVING_FEAST));
        var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false);
        player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        test.assertTrue(done(test,player,"happy_thanksgiving"),"Actual feast placement counts without cooking prerequisites");
        test.succeed();
    }

    @GameTest(maxTicks=280,padding=16)
    public void furnaceAndSmokerCreditCookedRecipesNotPossession(GameTestHelper test) {
        floor(test); var furnacePlayer=test.makeMockServerPlayerInLevel(); var smokerPlayer=test.makeMockServerPlayerInLevel();
        furnacePlayer.getInventory().add(new ItemStack(TurkeyItems.COOKED_TURKEY));
        test.assertTrue(!done(test,furnacePlayer,"smells_good"),"Given cooked meat does not count");
        test.setBlock(2,1,2,Blocks.FURNACE);test.setBlock(4,1,2,Blocks.SMOKER);
        var furnace=(AbstractFurnaceBlockEntity)test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(2,1,2)));
        var smoker=(AbstractFurnaceBlockEntity)test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(4,1,2)));
        for (var block:List.of(furnace,smoker)) {block.setItem(0,new ItemStack(TurkeyItems.RAW_TURKEY));block.setItem(1,new ItemStack(Items.COAL));}
        test.runAtTickTime(240,()->{
            for (int i=0;i<2;i++) {
                var block=i==0?furnace:smoker; var cook=i==0?furnacePlayer:smokerPlayer;
                test.assertTrue(block.getItem(2).is(TurkeyItems.COOKED_TURKEY),"Real cooking finishes");
                var slot=new FurnaceResultSlot(cook,block,2,0,0);var result=slot.remove(1);slot.onTake(cook,result);
                test.assertTrue(done(test,cook,"smells_good"),"Collecting real cooked output counts");
            }
            test.succeed();
        });
    }

    @GameTest(maxTicks=650,padding=16)
    public void campfireCreditsTheCookAfterCompletion(GameTestHelper test) {
        floor(test);var cook=test.makeMockServerPlayerInLevel();
        test.setBlock(3,1,3,Blocks.CAMPFIRE);
        var fire=(CampfireBlockEntity)test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(3,1,3)));
        test.assertTrue(fire.placeFood(test.getLevel(),cook,new ItemStack(TurkeyItems.RAW_TURKEY)),"Campfire accepts raw turkey");
        test.assertTrue(!done(test,cook,"smells_good"),"Starting a campfire recipe does not count");
        var saved=fire.saveWithFullMetadata(test.getLevel().registryAccess());
        var restored=(CampfireBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                fire.getBlockPos(),fire.getBlockState(),saved,test.getLevel().registryAccess());
        test.assertTrue(restored!=null,"Campfire reloads from saved data");
        test.getLevel().setBlockEntity(restored);
        test.runAtTickTime(620,()->{
            test.assertTrue(restored.getItems().stream().allMatch(ItemStack::isEmpty),"Restored campfire recipe finishes");
            test.assertTrue(done(test,cook,"smells_good"),"Only completed campfire cooking credits the cook");
            test.succeed();
        });
    }

    private static final class Egg extends TurkeyEggProjectile {
        private final boolean hatch;
        Egg(ServerLevel level,boolean hatch) {super(TurkeyTime.TURKEY_EGG_PROJECTILE,level);this.hatch=hatch;}
        @Override protected boolean shouldHatch(){return hatch;}
        void impact(HitResult hit){onHit(hit);}
    }
    @GameTest(padding=16)
    public void hatcheryRequiresSuccessfulPlayerOwnedImpact(GameTestHelper test) {
        floor(test);var player=player(test);var bystander=player(test);
        var pos=test.absolutePos(new BlockPos(3,0,3));
        var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false);
        var failed=new Egg(test.getLevel(),false);failed.setOwner(player);failed.impact(hit);
        test.assertTrue(!done(test,player,"turkey_hatchery"),"Failed egg does not count");
        test.setBlock(3,1,3,Blocks.STONE);
        var blocked=new Egg(test.getLevel(),true);blocked.setOwner(player);blocked.impact(hit);
        test.assertTrue(!done(test,player,"turkey_hatchery"),"Blocked hatch does not count");
        test.setBlock(3,1,3,Blocks.AIR);
        var dispenser=new Egg(test.getLevel(),true);dispenser.impact(hit);
        test.assertTrue(!done(test,player,"turkey_hatchery"),"Ownerless hatch credits nobody");
        test.getEntities(TurkeyTime.TURKEY).forEach(t->t.discard());
        var success=new Egg(test.getLevel(),true);success.setOwner(player);success.impact(hit);success.impact(hit);
        test.assertTrue(done(test,player,"turkey_hatchery")&&!done(test,bystander,"turkey_hatchery"),"Only the successful thrower earns Hatchery");
        test.succeed();
    }

    @GameTest
    public void seasonalCalendarHandlesBoundariesDisableAndYearWrap(GameTestHelper test) {
        var config=new TurkeyConfig();
        for (var date:List.of("2026-11-01","2026-11-30"))test.assertTrue(SeasonalEvent.activeOn(config,LocalDate.parse(date)),"November endpoints included");
        for (var date:List.of("2026-10-31","2026-12-01"))test.assertTrue(!SeasonalEvent.activeOn(config,LocalDate.parse(date)),"Outside event excluded");
        config.enableSeasonalEvent=false;test.assertTrue(!SeasonalEvent.activeOn(config,LocalDate.parse("2026-11-15")),"Event disable wins");
        config.enableSeasonalEvent=true;config.seasonalEventStartMonthDay="12-20";config.seasonalEventEndMonthDay="01-05";
        test.assertTrue(SeasonalEvent.activeOn(config,LocalDate.parse("2027-01-01"))&&!SeasonalEvent.activeOn(config,LocalDate.parse("2027-06-01")),"Date ranges wrap across New Year");
        var clamped=TurkeyConfig.parse("{\"seasonalCosmeticChance\":3}");test.assertTrue(clamped.seasonalCosmeticChance==1,"Chance bounded");
        for(var json:List.of("{\"seasonalEventTimezone\":\"invalid-zone\"}","{\"seasonalEventStartMonthDay\":\"11-31\"}","{\"seasonalCosmeticChance\":\"0.5\"}")) {
            boolean bad=false;try{TurkeyConfig.parse(json);}catch(RuntimeException expected){bad=true;}test.assertTrue(bad,"Malformed seasonal config rejected");
        }
        test.succeed();
    }

    @GameTest(padding=16)
    public void autumnAppearancePersistsAndOldSavesStayOrdinary(GameTestHelper test) {
        var bird=test.spawn(TurkeyTime.TURKEY,3,1,3);bird.setAutumn(true);
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,test.getLevel().registryAccess());bird.save(output);
        var data=output.buildResult();bird.setAutumn(false);
        bird.load(TagValueInput.create(ProblemReporter.DISCARDING,test.getLevel().registryAccess(),data));
        test.assertTrue(bird.isAutumn(),"Autumn appearance survives save/load");
        data.remove("Autumn");bird.load(TagValueInput.create(ProblemReporter.DISCARDING,test.getLevel().registryAccess(),data));
        test.assertTrue(!bird.isAutumn(),"Existing pre-event save never randomly gains autumn plumage");
        data.remove("UUID");bird.setAutumn(true);
        bird.load(TagValueInput.create(ProblemReporter.DISCARDING,test.getLevel().registryAccess(),data));
        test.assertTrue(bird.isAutumn(),"New spawn templates preserve their server-rolled coat");
        test.succeed();
    }
}
