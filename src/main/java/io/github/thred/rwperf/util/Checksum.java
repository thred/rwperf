package io.github.thred.rwperf.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Checksum
{

    private final MessageDigest digest;

    public Checksum()
    {
        try
        {
            digest = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("O my god, they killed SHA-1!");
        }
    }

    public void update(byte[] buffer, int offset, int length)
    {
        digest.update(buffer, offset, length);
    }

    public String complete()
    {
        return Base64.getEncoder().encodeToString(digest.digest());
    }

}
