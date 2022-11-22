package fr.openent.presences.common.helper;

import fr.openent.presences.model.IModel;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.SubTypes;

@RunWith(VertxUnitRunner.class)
public class IModelHelperTest {
    enum MyEnum {
        VALUE1,
        VALUE2,
        VALUE3
    }

    class MyClass {
        public String id;
    }
    class MyIModel implements IModel<MyIModel> {
        public String id;
        public boolean isGood;
        public MyOtherIModel otherIModel;
        public MyClass myClass;
        public List<Integer> typeIdList;
        public List<MyOtherIModel> otherIModelList;
        public List<MyClass> myClassList;
        public List<List<JsonObject>> listList;
        public MyEnum myEnum;
        public List<MyEnum> myEnumList;
        public MyEnum nullValue = null;

        @Override
        public JsonObject toJson() {
            return IModelHelper.toJson(false, this);
        }

        @Override
        public boolean validate() {
            return false;
        }
    }

    class MyOtherIModel implements IModel<MyOtherIModel> {
        public String myName;

        @Override
        public JsonObject toJson() {
            return IModelHelper.toJson(true, this);
        }

        @Override
        public boolean validate() {
            return false;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(IModelHelperTest.class);

    @Test
    public void testSubClassIModel(TestContext ctx) {
        Reflections reflections = new Reflections("fr.openent.presences");
        List<Class<?>> ignoredClassList = Arrays.asList(MyIModel.class, MyOtherIModel.class);

        Set<Class<?>> subTypes =
                reflections.get(SubTypes.of(IModel.class).asClass());
        List<Class<?>> invalidModel = subTypes.stream()
                .filter(modelClass -> !ignoredClassList.contains(modelClass))
                .filter(modelClass -> {
            Constructor<?> emptyConstructor = Arrays.stream(modelClass.getConstructors())
                    .filter(constructor -> constructor.getParameterTypes().length == 1
                            && constructor.getParameterTypes()[0].equals(JsonObject.class))
                    .findFirst()
                    .orElse(null);
            return emptyConstructor == null;
        }).collect(Collectors.toList());

        invalidModel.forEach(modelClass -> {
            String message = String.format("[PresencesCommon@%s::testSubClassIModel]: The class %s must have public constructor with JsonObject parameter declared",
                    this.getClass().getSimpleName(), modelClass.getSimpleName());
            log.fatal(message);
        });

        ctx.assertTrue(invalidModel.isEmpty(), "One or more IModel don't have public constructor with JsonObject parameter declared. Check log above.");
    }

    @Test
    public void toJson(TestContext ctx) {
        MyOtherIModel otherIModel1 = new MyOtherIModel();
        otherIModel1.myName = "otherIModel1";
        MyOtherIModel otherIModel2 = new MyOtherIModel();
        otherIModel2.myName = "otherIModel2";
        MyOtherIModel otherIModel3 = new MyOtherIModel();
        otherIModel3.myName = "otherIModel3";

        MyClass myClass1 = new MyClass();
        myClass1.id = "myClass1";
        MyClass myClass2 = new MyClass();
        myClass2.id = "myClass2";
        MyClass myClass3 = new MyClass();
        myClass3.id = "myClass3";

        MyIModel iModel = new MyIModel();
        iModel.id = "id";
        iModel.isGood = true;
        iModel.typeIdList = Arrays.asList(1,2,3);
        iModel.otherIModel = otherIModel1;
        iModel.myClass = myClass1;
        iModel.otherIModelList = Arrays.asList(otherIModel2, otherIModel3);
        iModel.myClassList = Arrays.asList(myClass2, myClass3);

        iModel.listList = Arrays.asList(Arrays.asList(new JsonObject().put("uuid", "uuid1"), new JsonObject().put("uuid", "uuid2")),
                Arrays.asList(new JsonObject().put("uuid", "uuid3"), new JsonObject().put("uuid", "uuid4")),
                Arrays.asList(new JsonObject().put("uuid", "uuid5"), new JsonObject().put("uuid", "uuid6")));

        iModel.myEnum = MyEnum.VALUE1;
        iModel.myEnumList = Arrays.asList(MyEnum.VALUE2, MyEnum.VALUE3);

        String expected = "{\"id\":\"id\",\"is_good\":true,\"other_i_model\":{\"my_name\":\"otherIModel1\"}," +
                "\"type_id_list\":[1,2,3],\"other_i_model_list\":[{\"my_name\":\"otherIModel2\"},{\"my_name\":\"otherIModel3\"}]," +
                "\"my_class_list\":[],\"list_list\":[[{\"uuid\":\"uuid1\"},{\"uuid\":\"uuid2\"}],[{\"uuid\":\"uuid3\"}," +
                "{\"uuid\":\"uuid4\"}],[{\"uuid\":\"uuid5\"},{\"uuid\":\"uuid6\"}]],\"my_enum\":\"VALUE1\"," +
                "\"my_enum_list\":[\"VALUE2\",\"VALUE3\"],\"null_value\":null}";
        ctx.assertEquals(expected, iModel.toJson().toString());

        System.out.println();
    }
}