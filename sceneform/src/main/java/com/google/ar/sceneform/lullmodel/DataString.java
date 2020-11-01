// automatically generated by the FlatBuffers compiler, do not modify

package com.google.ar.sceneform.lullmodel;

import java.nio.*;

import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
/**
 * Data type for string values to be stored in a VariantDef.
 */
public final class DataString extends Table {
  public static DataString getRootAsDataString(ByteBuffer _bb) { return getRootAsDataString(_bb, new DataString()); }
  public static DataString getRootAsDataString(ByteBuffer _bb, DataString obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; vtable_start = bb_pos - bb.getInt(bb_pos); vtable_size = bb.getShort(vtable_start); }
  public DataString __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String value() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer valueAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer valueInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }

  public static int createDataString(FlatBufferBuilder builder,
      int valueOffset) {
    builder.startObject(1);
    DataString.addValue(builder, valueOffset);
    return DataString.endDataString(builder);
  }

  public static void startDataString(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addValue(FlatBufferBuilder builder, int valueOffset) { builder.addOffset(0, valueOffset, 0); }
  public static int endDataString(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

