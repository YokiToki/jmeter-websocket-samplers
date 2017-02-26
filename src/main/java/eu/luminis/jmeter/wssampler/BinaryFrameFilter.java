package eu.luminis.jmeter.wssampler;

import eu.luminis.websocket.BinaryFrame;
import eu.luminis.websocket.Frame;

import static eu.luminis.jmeter.wssampler.BinaryFrameFilter.ComparisonType.*;

public class BinaryFrameFilter extends FrameFilter {

    public enum ComparisonType {
        IsBinary,
        Equals,
        Contains,
        StartsWith,
        EndsWith,
        NotEquals,
        NotContains,
        NotStartsWith,
        NotEndsWith
    }

    ComparisonType filterType;
    Integer matchPosition;
    byte[] matchValue;

    public BinaryFrameFilter() {
        filterType = IsBinary;
    }

    public BinaryFrameFilter(ComparisonType type) {
        filterType = type;
        setComparisonType(type);
    }

    @Override
    protected void prepareFilter() {
        filterType = getComparisonType();
        switch (filterType) {
            case Contains:
            case NotContains:
                matchPosition = convertToInt(getMatchPosition());
            case Equals:
            case NotEquals:
            case StartsWith:
            case NotStartsWith:
            case EndsWith:
            case NotEndsWith:
                matchValue = BinaryUtils.parseBinaryString(getMatchValue());
                if (matchValue.length == 0)
                    log.error("Binary filter '" + getName() + "' is missing match value; will filter nothing!");
                break;
        }
    }

    private Integer convertToInt(String matchPosition) {
        try {
            return Integer.parseInt(matchPosition);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected boolean matchesFilter(Frame frame) {
        if (frame.isBinary()) {
            BinaryFrame receivedFrame = (BinaryFrame) frame;
            byte[] frameBytes = receivedFrame.getData();

            switch (filterType) {
                case IsBinary:
                    return true;
                case Equals:
                case NotEquals:
                    boolean equal = frameBytes.length == matchValue.length && equalBytes(frameBytes, 0, matchValue, 0, matchValue.length);
                    return filterType == Equals? equal: !equal;
                case Contains:
                case NotContains:
                    if (matchPosition != null) {
                        boolean contains;
                        if (matchValue.length > 0 && matchPosition + matchValue.length <= frameBytes.length)
                            contains = equalBytes(frameBytes, matchPosition, matchValue, 0, matchValue.length);
                        else
                            contains = false;
                        return filterType == Contains? contains: !contains;
                    }
                    else {
                        boolean contains = BinaryUtils.contains(frameBytes, matchValue);
                        return filterType == Contains? contains: !contains;
                    }
                case StartsWith:
                case NotStartsWith:
                    boolean startsWith = equalBytes(frameBytes, 0, matchValue, 0, matchValue.length);
                    return filterType == StartsWith? startsWith: !startsWith;
                case EndsWith:
                case NotEndsWith:
                    boolean endsWith = equalBytes(frameBytes, Math.max(0, frameBytes.length - matchValue.length), matchValue, 0, matchValue.length);
                    return filterType == EndsWith? endsWith: !endsWith;
                default:
                    throw new RuntimeException("unknown comparison type");
            }
        }
        else
            return false;
    }

    @Override
    public String toString() {
        return "Binary Frame Filter '" + getName() + "'";
    }

    static boolean equalBytes(byte[] frameBytes, int matchPosition, byte[] matchValue, int valuePosition, int length) {
        if (matchPosition + length > frameBytes.length || valuePosition + length > matchValue.length)
            return false;

        for (int i = 0; i < length; i++) {
            if (frameBytes[matchPosition + i] != matchValue[valuePosition + i])
                return false;
        }
        return true;
    }

    public ComparisonType getComparisonType() {
        return ComparisonType.valueOf(getPropertyAsString("comparisonType", "IsBinary"));
    }

    public void setComparisonType(ComparisonType type) {
        setProperty("comparisonType", type.toString());
    }

    public String getMatchPosition() {
        return getPropertyAsString("matchPosition");
    }

    public void setMatchPosition(String value) {
        setProperty("matchPosition", value);
    }

    public String getMatchValue() {
        return getPropertyAsString("matchValue");
    }

    public void setMatchValue(String value) {
        setProperty("matchValue", value);
    }
}
