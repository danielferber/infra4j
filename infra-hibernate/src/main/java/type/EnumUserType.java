package type;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.type.NullableType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * GenericEnumUserType.
 * Modified from http://www.hibernate.org/272.html.
 * @author jholloway
 */

@SuppressWarnings("deprecation")
public class EnumUserType implements UserType, ParameterizedType { // NOPMD
    private static final String DEFAULT_IDENTIFIER_METHOD_NAME = "name";
    private static final String DEFAULT_VALUE_OF_METHOD_NAME = "valueOf";
    
    @SuppressWarnings("rawtypes")
	private Class<? extends Enum> enumClass;
    private Class<?> identifierType;
    private Method identifierMethod;
    private Method valueOfMethod;
    private NullableType type;
    private int[] sqlTypes;

	/**
	 * Set parameter values. enum is the className of the enum
	 * identifierMethodName is the method name to use for retrieval type is the
	 * identifier type valueOfMethod is the method name to use for retrieval
	 * 
	 * @param parameters
	 *            are the parameters from which we obtain our values for this
	 *            type  
	 */
	public void setParameterValues(final Properties parameters) {
        String enumClassName = parameters.getProperty("enum");
        try {
            enumClass = Class.forName(enumClassName).asSubclass(Enum.class);
        } catch (ClassNotFoundException cfne) {
            throw new HibernateException("Enum class not found", cfne);
        }
        String identifierMethodName = parameters.getProperty("identifierMethod", DEFAULT_IDENTIFIER_METHOD_NAME);
        try {
            identifierMethod = enumClass.getMethod(identifierMethodName, new Class[0]);
            identifierType = identifierMethod.getReturnType();
        } catch (Exception e) {
            throw new HibernateException("Failed to obtain identifier method", e);
        }
//        type = (NullableType) TypeFactory.basic(identifierType.getName());
        if (type == null) {
            throw new HibernateException("Unsupported identifier type " + identifierType.getName());
        }
        sqlTypes = new int[] { type.sqlType() };
        String valueOfMethodName = parameters.getProperty("valueOfMethod", DEFAULT_VALUE_OF_METHOD_NAME);
        try {
            valueOfMethod = enumClass.getMethod(valueOfMethodName,new Class[] { identifierType });
        } catch (Exception e) {
            throw new HibernateException("Failed to obtain valueOf method", e);
        }
    }
    
    @SuppressWarnings("rawtypes")
	public Class returnedClass() {
        return enumClass;
    }
    
    public Object nullSafeGet(final ResultSet resultSet,
    		                  final String[] names,
    		                  final Object owner) throws HibernateException, SQLException {
        Object identifier = type.get(resultSet, names[0]);
        if (resultSet.wasNull()) {
            return null;
        }
        try {
            return valueOfMethod.invoke(enumClass, new Object[] { identifier });
        } catch (Exception e) {
            throw new HibernateException("Exception while invoking "
                + "valueOf method " + valueOfMethod.getName() + " of "
                + "enumeration class " + enumClass, e);
        }
    }

    public void nullSafeSet(final PreparedStatement statement, final Object value, final int index) throws HibernateException, SQLException {
        try {
            if (value == null) {
                statement.setNull(index, type.sqlType());
            } else {
                Object identifier = identifierMethod.invoke(value,
                    new Object[0]);
                type.set(statement, identifier, index);
            }
        } catch (Exception e) {
            throw new HibernateException("Exception invoking id method "
            		                     + identifierMethod.getName()
            		                     + " of enum class "
            		                     + enumClass, e);
        }
    }

    public int[] sqlTypes() { //NOPMD
        return sqlTypes;
    }

    public Object assemble(final Serializable cached,
    					   final Object owner) throws HibernateException {
        return cached;
    }

    public Object deepCopy(final Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(final Object value) throws HibernateException {
        return (Serializable) value;
    }

    public boolean equals(final Object objectA, final Object objectY) throws HibernateException { // NOPMD
        return objectA == objectY;
    }

    public int hashCode(final Object objectX) throws HibernateException {
        return objectX.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
        return original;
    }
} 

