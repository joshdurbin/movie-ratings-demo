package io.durbs.movieratings.codec

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.UnsafeInput
import com.esotericsoftware.kryo.io.UnsafeOutput
import com.esotericsoftware.kryo.pool.KryoCallback
import com.esotericsoftware.kryo.pool.KryoPool
import com.google.common.base.Charsets
import com.lambdaworks.redis.codec.RedisCodec
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants

import java.nio.ByteBuffer

@CompileStatic
@Slf4j
class KryoRedisCodec<T> implements RedisCodec<String, T> {

  KryoPool kryoPool

  Class<T> type

  KryoRedisCodec(Class<T> type, KryoPool kryoPool) {
    this.type = type
    this.kryoPool = kryoPool
  }

  @Override
  String decodeKey(ByteBuffer bytes) {

    Charsets.UTF_8.decode(bytes).toString()
  }

  @Override
  T decodeValue(ByteBuffer byteBuffer) {

    T value = null

    byte[] bytes = new byte[byteBuffer.remaining()]
    byteBuffer.get(bytes)

    kryoPool.run(new KryoCallback<T>() {

      @Override
      T execute(Kryo kryo) {
        value = kryo.readObject(new UnsafeInput(bytes), type)
      }
    })
  }

  @Override
  ByteBuffer encodeKey(String key) {

    Charsets.UTF_8.encode(key)
  }

  @Override
  ByteBuffer encodeValue(T object) {

    UnsafeOutput output = new UnsafeOutput(new ByteArrayOutputStream(), Constants.DEFAULT_KRYO_BUFFER)

    Kryo kryo = null

    try {

      kryo = kryoPool.borrow()
      kryo.writeObject(output, object)

    } catch (Exception exception) {

      log.error("An error ocurred attempting to encode ${object}", exception)

    } finally {

      if (kryo) {

        kryoPool.release(kryo)
      }
    }

    ByteBuffer.wrap(output.toBytes())
  }

}
