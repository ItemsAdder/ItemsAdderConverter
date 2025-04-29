package com.itemsadder.converter;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class MappingBuilder
{
    private final Mapping dsl;
    private final List<String> fromPaths;

    public MappingBuilder(Mapping dsl, List<String> fromPaths)
    {
        this.dsl = dsl;
        this.fromPaths = fromPaths;
    }

    public MappingTargetBuilder to(String targetPath)
    {
        return new MappingTargetBuilder(dsl, fromPaths, targetPath);
    }

    public static class MappingTargetBuilder
    {
        private final Mapping dsl;
        private final List<String> fromPaths;
        private final String toPath;
        private Function<Object, Object> transform = Function.identity();
        private Predicate<Object> condition = v -> true;

        public MappingTargetBuilder(Mapping dsl, List<String> fromPaths, String toPath)
        {
            this.dsl = dsl;
            this.fromPaths = fromPaths;
            this.toPath = toPath;
        }

        public MappingTargetBuilder transform(Function<Object, Object> transformer)
        {
            this.transform = transformer;
            return this;
        }

        public MappingTargetBuilder onlyIf(Predicate<Object> condition)
        {
            this.condition = condition;
            return this;
        }

        public Mapping end()
        {
            dsl.addEntry(new Mapping.MappingEntry(fromPaths, toPath, transform, condition));
            return dsl;
        }
    }
}
