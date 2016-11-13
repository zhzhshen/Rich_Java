import org.junit.Before;
import org.junit.Test;
import rich.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RollCommandTest {
    private final double INITIAL_BALANCE = 1000;
    private final double ESTATE_PICE = 200;
    private GameMap map;
    private Dice dice;
    private Place estate;
    private Player player;
    private Player otherPlayer;

    @Before
    public void before() {
        map = mock(GameMap.class);
        dice = mock(Dice.class);
        estate = new Estate(ESTATE_PICE);
        player = new Player(map, INITIAL_BALANCE);
    }
    @Test
    public void should_move_user_to_correspond_place() {
        when(map.move(eq(player), anyInt())).thenReturn(estate);

        player.execute(new RollCommand(dice));

        assertThat(player.getCurrentPlace(), is(estate));
    }

    @Test
    public void should_wait_for_respond_if_walk_to_empty_estate() {
        when(map.move(eq(player), anyInt())).thenReturn(estate);

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_COMMAND));

        player.execute(new RollCommand(dice));

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_RESPONSE));
    }

    @Test
    public void should_turn_end_if_walk_to_empty_estate_and_respond_no() {
        should_wait_for_respond_if_walk_to_empty_estate();

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_RESPONSE));

        player.respond(BuyLandRespond.No);

        assertThat(player.getStatus(), is(Player.Status.TURN_END));
    }

    @Test
    public void should_turn_end_if_walk_to_empty_estate_and_respond_yes() {
        should_wait_for_respond_if_walk_to_empty_estate();

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_RESPONSE));

        player.respond(BuyLandRespond.YesToBuy);

        assertThat(estate.getOwner(), is(player));
        assertThat(player.getBalance(), is(INITIAL_BALANCE - ESTATE_PICE));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));
    }

    @Test
    public void should_turn_end_if_walk_to_empty_estate_and_respond_yes_without_enough_money() {
        should_wait_for_respond_if_walk_to_empty_estate();

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_RESPONSE));

        player.reduceMoney(INITIAL_BALANCE);
        player.respond(BuyLandRespond.YesToBuy);

        assertThat(player.getBalance(), is(0.0));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));
    }

    @Test
    public void should_wait_for_respond_if_walk_to_own_estate() {
        when(map.move(eq(player), anyInt())).thenReturn(estate);

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_COMMAND));

        player.execute(new RollCommand(dice));

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_RESPONSE));
    }

    @Test
    public void should_turn_end_if_walk_to_own_estate_respond_no() {
        should_wait_for_respond_if_walk_to_own_estate();

        player.respond(BuyLandRespond.No);

        assertThat(player.getStatus(), is(Player.Status.TURN_END));
    }

    @Test
    public void should_turn_end_if_walk_to_own_estate_respond_yes() {
        should_wait_for_respond_if_walk_to_own_estate();

        player.respond(BuyLandRespond.YesToBuild);

        assertThat(estate.getLevel(), is(1));
        assertThat(player.getBalance(), is(INITIAL_BALANCE - ESTATE_PICE));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));
    }

    @Test
    public void should_turn_end_if_walk_to_own_estate_respond_yes_without_enough_money() {
        should_wait_for_respond_if_walk_to_own_estate();
        player.reduceMoney(INITIAL_BALANCE);

        player.respond(BuyLandRespond.YesToBuild);

        assertThat(estate.getLevel(), is(0));
        assertThat(player.getBalance(), is(0.0));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));
    }

    @Test
    public void should_turn_end_if_walk_to_own_estate_respond_yes_with_max_level() {
        should_wait_for_respond_if_walk_to_own_estate();
        estate.build();
        estate.build();
        estate.build();
        assertThat(estate.getLevel(), is(3));

        player.respond(BuyLandRespond.YesToBuild);

        assertThat(estate.getLevel(), is(3));
        assertThat(player.getBalance(), is(INITIAL_BALANCE));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));
    }

    @Test
    public void should_wait_for_respond_if_walk_to_others_estate() {
        otherPlayer = new Player(map, INITIAL_BALANCE);
        estate.sellTo(otherPlayer);
        when(map.move(eq(player), anyInt())).thenReturn(estate);

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_COMMAND));

        player.execute(new RollCommand(dice));

        assertThat(player.getStatus(), is(Player.Status.TURN_END));
    }

    public void walk_to_others_estate(int level) {
        otherPlayer = new Player(map, INITIAL_BALANCE);
        estate.sellTo(otherPlayer);
        for (int i = 0; i < level; i++) {
            estate.build();
        }
        when(map.move(eq(player), anyInt())).thenReturn(estate);

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_COMMAND));

        player.execute(new RollCommand(dice));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));
        assertThat(estate.getLevel(), is(level));
    }

    @Test
    public void should_pay_half_estate_price_if_walk_to_others_land_level_0() {
        walk_to_others_estate(0);

        assertThat(player.getBalance(), is(INITIAL_BALANCE - 0.5 * estate.getPrice()));
        assertThat(otherPlayer.getBalance(), is(INITIAL_BALANCE + 0.5 * estate.getPrice()));
    }

    @Test
    public void should_pay_estate_price_if_walk_to_others_land_level_1() {
        walk_to_others_estate(1);

        assertThat(player.getBalance(), is(INITIAL_BALANCE - estate.getPrice()));
        assertThat(otherPlayer.getBalance(), is(INITIAL_BALANCE + estate.getPrice()));
    }

    @Test
    public void should_pay_four_times_estate_price_if_walk_to_others_land_level_3() {
        walk_to_others_estate(3);

        assertThat(player.getBalance(), is(INITIAL_BALANCE - 4 * estate.getPrice()));
        assertThat(otherPlayer.getBalance(), is(INITIAL_BALANCE + 4 * estate.getPrice()));
    }

    @Test
    public void should_not_pay_if_walk_to_others_land_while_owner_is_in_hospital() {
        otherPlayer = new Player(map, INITIAL_BALANCE);
        estate.sellTo(otherPlayer);
        otherPlayer.burn();

        when(map.move(eq(player), anyInt())).thenReturn(estate);

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_COMMAND));

        player.execute(new RollCommand(dice));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));

        assertThat(player.getBalance(), is(INITIAL_BALANCE));
        assertThat(otherPlayer.getBalance(), is(INITIAL_BALANCE));
    }

    @Test
    public void should_not_pay_if_walk_to_others_land_while_owner_is_in_prison() {
        otherPlayer = new Player(map, INITIAL_BALANCE);
        estate.sellTo(otherPlayer);
        otherPlayer.prisoned();

        when(map.move(eq(player), anyInt())).thenReturn(estate);

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_COMMAND));

        player.execute(new RollCommand(dice));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));

        assertThat(player.getBalance(), is(INITIAL_BALANCE));
        assertThat(otherPlayer.getBalance(), is(INITIAL_BALANCE));
    }

    @Test
    public void should_not_pay_if_walk_to_others_land_while_has_evisu() {
        otherPlayer = new Player(map, INITIAL_BALANCE);
        estate.sellTo(otherPlayer);
        player.evisu();

        when(map.move(eq(player), anyInt())).thenReturn(estate);

        assertThat(player.getStatus(), is(Player.Status.WAIT_FOR_COMMAND));

        player.execute(new RollCommand(dice));
        assertThat(player.getStatus(), is(Player.Status.TURN_END));

        assertThat(player.getBalance(), is(INITIAL_BALANCE));
        assertThat(otherPlayer.getBalance(), is(INITIAL_BALANCE));
    }
}