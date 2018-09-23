package io.github.thred.rwperf.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class Arguments implements Iterable<String>, Cloneable
{

    private final List<String> args;

    public Arguments(String... args)
    {
        this(Arrays.asList(args));
    }

    public Arguments(List<String> args)
    {
        super();

        this.args = new ArrayList<>(args);
    }

    @Override
    public Arguments clone()
    {
        return new Arguments(new ArrayList<>(args));
    }

    /**
     * Adds an argument at the end of the list of arguments
     *
     * @param argument the argument
     * @return the {@link Arguments} itself
     */
    public Arguments add(String argument)
    {
        return add(args.size(), argument);
    }

    /**
     * Adds an argument at the specified index
     *
     * @param index the index
     * @param argument the argument
     * @return the {@link Arguments} itself
     */
    public Arguments add(int index, String argument)
    {
        args.add(index, argument);

        return this;
    }

    /**
     * Returns true if no argument is available (anymore).
     *
     * @return true if empty
     */
    public boolean isEmpty()
    {
        return args.isEmpty();
    }

    /**
     * Returns the number of (remaining) arguments.
     *
     * @return the number of (remaining) arguments
     */
    public int size()
    {
        return args.size();
    }

    /**
     * Returns the first index of one of the specified arguments.
     *
     * @param keys the arguments
     * @return the first index, -1 if none was found
     */
    public int indexOf(String... keys)
    {
        int index = Integer.MAX_VALUE;

        for (String key : keys)
        {
            int currentIndex = args.indexOf(key);

            if (currentIndex >= 0 && currentIndex < index)
            {
                index = currentIndex;
            }
        }

        return index < Integer.MAX_VALUE ? index : -1;
    }

    /**
     * Returns the last index of one of the specified arguments.
     *
     * @param keys the arguments
     * @return the last index, -1 if none was found
     */
    public int lastIndexOf(String... keys)
    {
        int index = -1;

        for (String key : keys)
        {
            int currentIndex = args.lastIndexOf(key);

            if (currentIndex > index)
            {
                index = currentIndex;
            }
        }

        return index;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<String> iterator()
    {
        return args.iterator();
    }

    @SuppressWarnings("unchecked")
    protected <Any> Any convert(String value, Class<Any> type)
    {
        if (value == null)
        {
            return null;
        }

        if (type.isAssignableFrom(String.class))
        {
            return (Any) value;
        }

        if (type.isAssignableFrom(Boolean.class))
        {
            return (Any) Boolean.valueOf(Boolean.parseBoolean(value));
        }

        if (type.isAssignableFrom(Byte.class))
        {
            return (Any) Byte.decode(value);
        }

        if (type.isAssignableFrom(Short.class))
        {
            return (Any) Short.decode(value);
        }

        if (type.isAssignableFrom(Integer.class))
        {
            return (Any) Integer.decode(value);
        }

        if (type.isAssignableFrom(Long.class))
        {
            return (Any) Long.decode(value);
        }

        if (type.isAssignableFrom(Float.class))
        {
            return (Any) Float.valueOf(Float.parseFloat(value));
        }

        if (type.isAssignableFrom(Double.class))
        {
            return (Any) Double.valueOf(Double.parseDouble(value));
        }

        if (type.isAssignableFrom(Character.class))
        {
            if (value.length() > 1)
            {
                throw new IllegalArgumentException("Character expected");
            }

            return (Any) Character.valueOf(value.charAt(0));
        }

        throw new UnsupportedOperationException("Type not supported: " + type);
    }

    /**
     * Returns a new {@link Arguments} object with all arguments, starting at the current index. The arguments will be
     * removed from the original list of arguments.
     *
     * @return a new {@link Arguments} object
     */
    public Arguments consumeAll()
    {
        return consumeAll(0);
    }

    /**
     * Returns a new {@link Arguments} object with all arguments, starting at the specified index. The arguments will be
     * removed from the original list of arguments.
     *
     * @param startIndex the start index
     * @return a new {@link Arguments} object
     */
    public Arguments consumeAll(int startIndex)
    {
        List<String> result = new ArrayList<>();

        while (startIndex < args.size())
        {
            result.add(args.remove(startIndex));
        }

        return new Arguments(result);
    }

    public <Any> Optional<Any> consume(Class<Any> type)
    {
        return consume(0, type);
    }

    @SuppressWarnings("unchecked")
    public <Any> Optional<Any> consume(int index, Class<Any> type)
    {
        if (isEmpty())
        {
            return Optional.empty();
        }

        if (index >= size())
        {
            return Optional.empty();
        }

        if (type.isArray())
        {
            return (Optional<Any>) consumeArray(index, type.getComponentType());
        }

        return Optional.of(convert(args.remove(index), type));
    }

    @SuppressWarnings("unchecked")
    public <Any> Optional<Any[]> consumeArray(int index, Class<Any> componentType)
    {
        Optional<Any> value = consume(index, componentType);

        if (!value.isPresent())
        {
            return Optional.empty();
        }

        List<Any> list = new ArrayList<>();

        while (value.isPresent())
        {
            list.add(value.get());
            value = consume(index, componentType);
        }

        return Optional.of(list.<Any> toArray((Any[]) Array.newInstance(componentType, list.size())));
    }

    /**
     * Searches for the specified argument. If found, removes it and returns and removes the next argument.
     *
     * @param <Any> the type of argument to consume
     * @param key the argument
     * @param type the type of the argument
     * @return the value part (next argument), null if key was not found
     */
    public <Any> Optional<Any> consume(String key, Class<Any> type)
    {
        int indexOf = args.indexOf(key);

        if (indexOf < 0)
        {
            return Optional.empty();
        }

        args.remove(indexOf);

        if (indexOf >= args.size())
        {
            throw new IllegalArgumentException(String.format("Invalid argument: %s. Value is missing.", key));
        }

        return consume(indexOf, type);
    }

    /**
     * Consumes the specified argument.
     *
     * @param flag the argument
     * @return true if the argument was found, false otherwise.
     */
    public boolean consumeFlag(String flag)
    {
        return args.remove(flag);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        for (String arg : args)
        {
            if (builder.length() > 0)
            {
                builder.append(" ");
            }

            builder.append(arg);
        }

        return builder.toString();
    }
}
