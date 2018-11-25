package lucee.runtime.listener;

import lucee.commons.lang.StringUtil;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;

public class SerializationSettings {

    public static int SERIALIZE_AS_ROW = 1;
    public static int SERIALIZE_AS_COLUMN = 2;
    public static int SERIALIZE_AS_STRUCT = 4;

    private int serializeQueryAs = SERIALIZE_AS_ROW;

    public static final SerializationSettings DEFAULT = new SerializationSettings(SERIALIZE_AS_ROW);

    public SerializationSettings(int serializeQueryAs) {
	this.serializeQueryAs = serializeQueryAs;
    }

    public int getSerializeQueryAs() {
	return serializeQueryAs;
    }

    public static int toSerializeQueryAs(String str) {
	if (StringUtil.isEmpty(str)) return SERIALIZE_AS_ROW;
	str = str.trim();
	if ("column".equalsIgnoreCase(str)) return SERIALIZE_AS_COLUMN;
	if ("struct".equalsIgnoreCase(str)) return SERIALIZE_AS_STRUCT;
	return SERIALIZE_AS_ROW;
    }

    public static String toSerializeQueryAs(int i) {
	if (i == SERIALIZE_AS_COLUMN) return "column";
	if (i == SERIALIZE_AS_STRUCT) return "struct";
	return "row";
    }

    public static SerializationSettings toSerializationSettings(Struct sct) {
	return new SerializationSettings(toSerializeQueryAs(Caster.toString(sct.get("serializeQueryAs", null), null)));
    }

    public Object toStruct() {
	Struct sct = new StructImpl();
	sct.setEL("serializeQueryAs", toSerializeQueryAs(serializeQueryAs));

	return sct;
    }
}
