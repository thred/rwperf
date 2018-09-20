package io.github.thred.rwperf;

public class AverageConsumer implements Consumer
{

    private class Average
    {
        private final Operation operation;

        private long size = 0;
        private double duration = 0;

        public Average(Operation operation)
        {
            super();

            this.operation = operation;
        }

        public void consume(Result result)
        {
            size += result.getSize();
            duration += result.getDuration();
        }

        @Override
        public String toString()
        {
            double result = size / duration;

            if (result > 1024 * 1024)
            {
                return String.format("%s: %,.3f mb/s", operation, result / (1024 * 1024));
            }

            if (result > 1024)
            {
                return String.format("%s: %,.3f kb/s", operation, result / 1024);
            }

            return String.format("%s: %,.3f b/s", operation, result);
        }
    }

    private final Average writeAverage = new Average(Operation.Write);
    private final Average readAverage = new Average(Operation.Read);
    private final Average deleteAverage = new Average(Operation.Delete);

    @Override
    public void consume(Result result)
    {
        switch (result.getOperation())
        {
            case Write:
                writeAverage.consume(result);
                break;

            case Read:
                readAverage.consume(result);
                break;

            case Delete:
                deleteAverage.consume(result);
                break;

            default:
                throw new UnsupportedOperationException("Operation not supported: " + result.getOperation());
        }
    }

    @Override
    public String toString()
    {
        return writeAverage + "\n" + readAverage + "\n" + deleteAverage;
    }

}
