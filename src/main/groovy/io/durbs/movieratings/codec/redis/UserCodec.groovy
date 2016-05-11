package io.durbs.movieratings.codec.redis

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.UnsafeInput
import com.esotericsoftware.kryo.io.UnsafeOutput
import com.esotericsoftware.kryo.pool.KryoCallback
import com.esotericsoftware.kryo.pool.KryoPool
import com.google.common.base.Charsets
import com.google.inject.Inject
import com.google.inject.Singleton
import com.lambdaworks.redis.codec.RedisCodec
import groovy.transform.CompileStatic
import io.durbs.movieratings.model.persistent.User

import java.nio.ByteBuffer

@Singleton
@CompileStatic
class UserCodec implements RedisCodec<String, User> {

  static final int DEFAULT_BUFFER = 1024 * 100

  @Inject
  KryoPool kryoPool

  @Override
  String decodeKey(final ByteBuffer bytes) {

    Charsets.UTF_8.decode(bytes).toString()
  }

  @Override
  User decodeValue(final ByteBuffer byteBuffer) {

    final byte[] bytes = new byte[byteBuffer.remaining()]
    byteBuffer.get(bytes)

    kryoPool.run(new KryoCallback<User>() {

      @Override
      User execute(Kryo kryo) {
        kryo.readObject(new UnsafeInput(bytes), User)
      }
    })
  }

  @Override
  ByteBuffer encodeKey(final String key) {

    Charsets.UTF_8.encode(key)
  }

  @Override
  ByteBuffer encodeValue(final User user) {

    final UnsafeOutput output = new UnsafeOutput(new ByteArrayOutputStream(), DEFAULT_BUFFER)

    final Kryo kryo = kryoPool.borrow()
    kryo.writeObject(output, user)
    kryoPool.release(kryo)

    ByteBuffer.wrap(output.toBytes())
  }
}