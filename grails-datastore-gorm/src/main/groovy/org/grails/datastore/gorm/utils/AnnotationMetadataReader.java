package org.grails.datastore.gorm.utils;
/*
 * Copyright 2016-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;

/**
 * A more limited version of Spring's annotation reader that only reads annotations on classes
 *
 * @author Graeme Rocher
 * @since 3.1.13
 */
public class AnnotationMetadataReader implements MetadataReader {
    private final Resource resource;

    private final ClassMetadata classMetadata;

    private final AnnotationMetadata annotationMetadata;

    /**
     * Constructs a new annotation metadata reader with the attributes
     *
     * @param resource The resource
     * @throws IOException
     */
    AnnotationMetadataReader(Resource resource) throws IOException {
        this.annotationMetadata = AnnotationMetadata.introspect(resource.getClass());
        // since AnnotationMetadata extends ClassMetadata
        this.classMetadata = this.annotationMetadata;
        this.resource = resource;
    }

    @Override
    public Resource getResource() {
        return this.resource;
    }

    @Override
    public ClassMetadata getClassMetadata() {
        return this.classMetadata;
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return this.annotationMetadata;
    }

    private static class EmptyAnnotationVisitor extends AnnotationVisitor {

        EmptyAnnotationVisitor() {
            super(loadAsmVersion());
        }

        private static int loadAsmVersion()  {
            try {
                return (int)SpringAsmInfo.class.getField("ASM_VERSION").get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            return this;
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return this;
        }
    }
}
