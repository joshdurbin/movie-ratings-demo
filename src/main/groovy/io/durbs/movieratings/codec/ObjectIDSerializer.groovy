package io.durbs.movieratings.codec

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.bson.types.ObjectId

@CompileStatic
@Slf4j
class ObjectIDSerializer extends JsonSerializer<ObjectId> {

  @Override
  void serialize(ObjectId objectID, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

    objectID ? jsonGenerator.writeString(objectID.toString()) : jsonGenerator.writeNull()
  }
}
