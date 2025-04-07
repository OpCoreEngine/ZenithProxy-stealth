package com.zenith.command.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zenith.api.command.*;
import org.jspecify.annotations.NonNull;

import java.util.function.Predicate;

public class CaseInsensitiveLiteralArgumentBuilder<S> extends LiteralArgumentBuilder<S> {
    private static final CommandErrorHandler DEFAULT_ERROR_HANDLER = (m, c) -> {};
    private static final CommandSuccessHandler DEFAULT_SUCCESS_HANDLER = (c) -> {};
    private static final CommandExecutionErrorHandler DEFAULT_EXECUTION_ERROR_HANDLER = (c) -> {};

    private @NonNull CommandErrorHandler errorHandler = DEFAULT_ERROR_HANDLER;
    private @NonNull CommandSuccessHandler successHandler = DEFAULT_SUCCESS_HANDLER;
    private @NonNull CommandExecutionErrorHandler executionErrorHandler = DEFAULT_EXECUTION_ERROR_HANDLER;

    protected CaseInsensitiveLiteralArgumentBuilder(String literal) {
        super(literal);
    }

    public static <S> CaseInsensitiveLiteralArgumentBuilder<S> literal(final String name) {
        return new CaseInsensitiveLiteralArgumentBuilder<>(name);
    }

    public static <S> CaseInsensitiveLiteralArgumentBuilder<S> literal(final String name, CommandErrorHandler errorHandler) {
        final CaseInsensitiveLiteralArgumentBuilder<S> builder = literal(name);
        return builder.withErrorHandler(errorHandler);
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> withErrorHandler(CommandErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> withSuccessHandler(CommandSuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> withExecutionErrorHandler(CommandExecutionErrorHandler errorHandler) {
        this.executionErrorHandler = errorHandler;
        return this;
    }

    public CaseInsensitiveLiteralArgumentBuilder<S> requires(final Predicate<S> requirement) {
        super.requires(requirement);
        return this;
    }

    public LiteralArgumentBuilder<S> executes(final IExecutes<S> command) {
        return this.executes((context) -> {
            command.execute(context);
            return Command.OK;
        });
    }

    @Override
    public LiteralCommandNode<S> build() {
        final LiteralCommandNode<S> result = new CaseInsensitiveLiteralCommandNode<>(
            getLiteral(),
            getCommand(),
            getRequirement(),
            getRedirect(),
            getRedirectModifier(),
            isFork(),
            errorHandler,
            successHandler,
            executionErrorHandler);

        for (final CommandNode<S> argument : getArguments()) {
            result.addChild(argument);
        }

        return result;
    }
}
