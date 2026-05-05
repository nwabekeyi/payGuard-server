package xyz.outlinr.api.utils;

import java.util.UUID;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Hibernate IdentifierGenerator that produces UUID version 7.
 */
public class UUIDv7IdentifierGenerator implements IdentifierGenerator {

    @Override
    public UUID generate(SharedSessionContractImplementor session, Object object) {
        return UUIDv7Generator.generate();
    }
}