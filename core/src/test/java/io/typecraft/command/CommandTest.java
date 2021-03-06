package io.typecraft.command;


import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.typecraft.command.Argument.intArg;
import static io.typecraft.command.Argument.strArg;
import static io.typecraft.command.Command.pair;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandTest {
    private static final Argument<Integer> intTabArg = intArg.withTabCompleter(() -> Arrays.asList("10", "20"));
    // MyCommand = AddItem | RemoveItem | ...
    private static final Command.Mapping<MyCommand> itemCommand =
            Command.mapping(
                    pair("open", Command.present(new OpenItemList()).withDescription("아이템 목록을 엽니다.")),
                    // intArg: Argument<Integer>
                    // strArg: Argument<String>
                    pair("add", Command.argument(AddItem::new, intArg, strArg)),
                    pair("remove", Command.argument(RemoveItem::new, intArg)),
                    pair("page", Command.argument(PageItem::new, intTabArg)),
                    pair("lazy", Command.present(new AddItem(0, null)))
            );
    private static final Command.Mapping<MyCommand> itemCommandWithFallback =
            itemCommand.withFallback(Command.present(new FallbackItem()));
    private static final Command<MyCommand> reloadCommand =
            Command.<MyCommand>present(new ReloadCommand()).withDescription("리로드합니다.");
    private static final Command<MyCommand> rootCommand =
            Command.mapping(
                    pair("item", itemCommand),
                    pair("reload", reloadCommand)
            );

    public interface MyCommand {
    }

    public static class AddItem implements MyCommand {
        private final int index;
        private final String name;

        public AddItem(int index, String name) {
            this.index = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return "AddItem{" +
                    "index=" + index +
                    ", name='" + name + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AddItem addItem = (AddItem) o;
            return index == addItem.index && Objects.equals(name, addItem.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, name);
        }
    }

    public static class RemoveItem implements MyCommand {
        public final Number index;

        public RemoveItem(Number index) {
            this.index = index;
        }
    }

    public static class PageItem implements MyCommand {
        public final int index;

        public PageItem(int index) {
            this.index = index;
        }
    }

    public static class OpenItemList implements MyCommand {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof OpenItemList;
        }
    }

    public static class FallbackItem implements MyCommand {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof FallbackItem;
        }
    }

    public static class ReloadCommand implements MyCommand {
    }

    // executor
    @Test
    public void unit() {
        String[] args = new String[0];
        assertEquals(
                Either.left(new CommandFailure.FewArguments<>(args, 0, rootCommand)),
                Command.parse(args, rootCommand)
        );
    }

    @Test
    public void sub() {
        String[] args = new String[]{"item"};
        assertEquals(
                Either.left(new CommandFailure.FewArguments<>(args, 1, itemCommand)),
                Command.parse(args, rootCommand)
        );
    }

    @Test
    public void subUnknown() {
        String[] args = new String[]{"item", "unknownCommand"};
        assertEquals(
                Either.left(new CommandFailure.UnknownSubCommand<>(args, 1, itemCommand)),
                Command.parse(args, rootCommand)
        );
    }

    @Test
    public void present() {
        String[] args = new String[]{"item", "open"};
        assertEquals(
                Either.right(new CommandSuccess<>(args, 2, new OpenItemList())),
                Command.parse(args, rootCommand)
        );
    }

    @Test
    public void argument() {
        int index = 0;
        String name = "someName";
        String[] args = new String[]{"item", "add", String.valueOf(index), name};
        assertEquals(
                Either.right(new CommandSuccess<>(args, args.length, new AddItem(index, name))),
                Command.parse(args, rootCommand)
        );
    }

    @Test
    public void help() {
        String[] args = new String[]{"item", "a"};
        Either<CommandFailure<MyCommand>, CommandSuccess<MyCommand>> result = Command.parse(args, rootCommand);
        CommandFailure<MyCommand> failure = result.getLeft();
        if (failure instanceof CommandFailure.FewArguments) {
            CommandFailure.FewArguments<MyCommand> fewArgs = (CommandFailure.FewArguments<MyCommand>) failure;
            helpCommand(fewArgs.getArguments(), fewArgs.getIndex(), fewArgs.getCommand());
        } else if (failure instanceof CommandFailure.UnknownSubCommand) {
            CommandFailure.UnknownSubCommand<MyCommand> unknown = (CommandFailure.UnknownSubCommand<MyCommand>) failure;
            helpCommand(unknown.getArguments(), unknown.getIndex(), unknown.getCommand());
        }
    }

    private <A> void helpCommand(String[] args, int position, Command<A> cmd) {
        String[] succArgs = args.length >= 1
                ? Arrays.copyOfRange(args, 0, position)
                : new String[0];
        for (Map.Entry<List<String>, Command<A>> pair : Command.getEntries(cmd)) {
            List<String> usageArgs = pair.getKey().stream()
                    .flatMap(s -> Stream.concat(
                            Arrays.stream(succArgs),
                            Stream.of(s)
                    ))
                    .collect(Collectors.toList());
            String description = Command.getSpec(pair.getValue()).getDescriptionId().getMessage();
            String suffix = description.isEmpty()
                    ? ""
                    : " - " + description;
            System.out.println("/" + String.join(" ", usageArgs) + suffix);
        }
    }

    @Test
    public void mappingFallback() {
        String[] args = new String[]{"item"};
        Either<CommandFailure<MyCommand>, CommandSuccess<MyCommand>> result =
                Command.parse(args, Command.mapping(
                        pair("item", itemCommandWithFallback)
                ));
        assertEquals(
                Either.right(new CommandSuccess<>(args, 2, new FallbackItem())),
                result
        );
    }

    // tab
    @Test
    public void tabUnit() {
        String[] args = new String[0];
        assertEquals(
                CommandTabResult.suggestion(Arrays.asList("item", "reload")),
                Command.tabComplete(args, rootCommand)
        );
    }

    @Test
    public void tabSub() {
        String[] args = new String[]{"item", ""};
        assertEquals(
                CommandTabResult.suggestion(Arrays.asList("open", "add", "remove", "page", "lazy")),
                Command.tabComplete(args, rootCommand)
        );
    }

    @Test
    public void tabSubUnknown() {
        String[] args = new String[]{"item", "unknownCommand"};
        assertEquals(
                CommandTabResult.suggestion(Collections.emptyList()),
                Command.tabComplete(args, rootCommand)
        );
    }

    @Test
    public void tabCustom() {
        assertEquals(
                CommandTabResult.suggestion(Arrays.asList("10", "20")),
                Command.tabComplete(new String[]{"item", "page", ""}, rootCommand)
        );
        assertEquals(
                CommandTabResult.suggestion(Collections.singletonList("10")),
                Command.tabComplete(new String[]{"item", "page", "1"}, rootCommand)
        );
    }
}
