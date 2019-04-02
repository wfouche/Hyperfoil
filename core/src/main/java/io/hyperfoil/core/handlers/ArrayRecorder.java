package io.hyperfoil.core.handlers;

import java.nio.charset.StandardCharsets;

import org.kohsuke.MetaInfServices;

import io.hyperfoil.api.config.Locator;
import io.hyperfoil.api.connection.Request;
import io.hyperfoil.api.http.Processor;
import io.netty.buffer.ByteBuf;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.core.session.ObjectVar;
import io.hyperfoil.api.session.ResourceUtilizer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ArrayRecorder implements Processor<Request>, ResourceUtilizer {
   private static final Logger log = LoggerFactory.getLogger(ArrayRecorder.class);
   private final String var;
   private final int maxSize;

   public ArrayRecorder(String var, int maxSize) {
      this.var = var;
      this.maxSize = maxSize;
   }

   public void before(Request request) {
      ObjectVar[] array = (ObjectVar[]) request.session.activate(var);
      for (int i = 0; i < array.length; ++i) {
         array[i].unset();
      }
   }

   @Override
   public void process(Request request, ByteBuf data, int offset, int length, boolean isLastPart) {
      assert isLastPart;
      ObjectVar[] array = (ObjectVar[]) request.session.activate(var);
      String value = data.toString(offset, length, StandardCharsets.UTF_8);
      for (int i = 0; i < array.length; ++i) {
         if (array[i].isSet()) continue;
         array[i].set(value);
         return;
      }
      log.warn("Exceed maximum size of the array {} ({}), dropping value {}", var, maxSize, value);
   }

   @Override
   public void reserve(Session session) {
      session.declare(var);
      session.setObject(var, ObjectVar.newArray(session, maxSize));
      session.unset(var);
   }

   public static class Builder implements Processor.Builder<Request> {
      private String var;
      private int maxSize;

      @Override
      public ArrayRecorder build() {
         return new ArrayRecorder(var, maxSize);
      }

      public Builder var(String var) {
         this.var = var;
         return this;
      }

      public Builder maxSize(int maxSize) {
         this.maxSize = maxSize;
         return this;
      }
   }

   @MetaInfServices(Request.ProcessorBuilderFactory.class)
   public static class BuilderFactory implements Request.ProcessorBuilderFactory {
      @Override
      public String name() {
         return "array";
      }

      @Override
      public boolean acceptsParam() {
         return false;
      }

      @Override
      public Builder newBuilder(Locator locator, String param) {
         return new Builder();
      }
   }
}
