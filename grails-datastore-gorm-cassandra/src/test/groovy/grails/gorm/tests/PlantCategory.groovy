package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Entity
class PlantCategory implements Serializable {
    UUID id
    Long version
    Set plants
    String name

    static hasMany = [plants:Plant]

    static namedQueries = {
//        withPlantsInPatch {
//            plants {
//                eq 'goesInPatch', true
//            }
//        }
//        withPlantsThatStartWithG {
//            plants {
//                like 'name', 'G%'
//            }
//        }
//        withPlantsInPatchThatStartWithG {
//            withPlantsInPatch()
//            withPlantsThatStartWithG()
//        }
    }
}