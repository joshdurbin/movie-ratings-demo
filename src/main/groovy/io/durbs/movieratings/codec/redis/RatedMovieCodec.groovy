package io.durbs.movieratings.codec.redis

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.pool.KryoCallback
import com.esotericsoftware.kryo.pool.KryoPool
import com.google.common.base.Charsets
import com.google.inject.Inject
import com.google.inject.Singleton
import com.lambdaworks.redis.codec.RedisCodec
import groovy.transform.CompileStatic
import io.durbs.movieratings.model.RatedMovie

import java.nio.ByteBuffer

@Singleton
@CompileStatic
class RatedMovieCodec implements RedisCodec<String, RatedMovie> {

  static final int DEFAULT_BUFFER = 1024 * 100

  @Inject
  KryoPool kryoPool

  @Override
  String decodeKey(final ByteBuffer bytes) {

    Charsets.UTF_8.decode(bytes).toString()
  }

  @Override
  RatedMovie decodeValue(final ByteBuffer byteBuffer) {

    final byte[] bytes = new byte[byteBuffer.remaining()]
    byteBuffer.get(bytes)

    kryoPool.run(new KryoCallback<RatedMovie>() {

      @Override
      RatedMovie execute(Kryo kryo) {
        kryo.readObject(new Input(bytes), RatedMovie)
      }
    })
  }

  @Override
  ByteBuffer encodeKey(final String key) {

    Charsets.UTF_8.encode(key)
  }

  @Override
  ByteBuffer encodeValue(final RatedMovie ratedMovie) {

    final Output output = new Output(new ByteArrayOutputStream(), DEFAULT_BUFFER)

    final Kryo kryo = kryoPool.borrow()
    kryo.writeObject(output, ratedMovie)
    kryoPool.release(kryo)

    ByteBuffer.wrap(output.toBytes())
  }
}