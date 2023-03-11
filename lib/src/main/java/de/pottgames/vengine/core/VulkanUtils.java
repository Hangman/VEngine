package de.pottgames.vengine.core;

public class VulkanUtils {
    public static final int UINT32_MAX = 0xFFFFFFFF;


    public record ApiVersion(int major, int minor, int patch, int variant) {

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(this.major);
            builder.append(".");
            builder.append(this.minor);
            builder.append(".");
            builder.append(this.patch);
            return builder.toString();
        }

    }


    public static ApiVersion decodeApiVersionNumber(int number) {
        final long version = number & 0xFFFFFFFF;
        final int major = (int) (version >>> 22 & 0b1111111);
        final int minor = (int) (version >>> 12 & 0b1111111111);
        final int patch = (int) (version & 0b111111111111);
        final int variant = (int) (version >>> 29 & 0b111);
        return new ApiVersion(major, minor, patch, variant);
    }

}
