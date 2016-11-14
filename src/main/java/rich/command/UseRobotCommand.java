package rich.command;

import com.sun.tools.javac.util.Pair;
import rich.Player;
import rich.response.Response;
import rich.item.Robot;

public class UseRobotCommand implements Command {
    public Pair<Player.Status, String> execute(Player player) {
        return Pair.of(Player.Status.WAIT_FOR_COMMAND, player.use(new Robot(), 0));
    }

    public Pair<Player.Status, String> respond(Player player, Response response) {
        return response.execute(player);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UseRobotCommand;
    }
}
