package com.aws.class3.dynamo.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DynamoService {

    private final DynamoDbClient dynamoDbClient;

    private static final String TABLE_NAME = "assurance-table";

    // Constantes para evitar erros de digitação nas chaves primárias
    private static final String PARTITION_KEY = "assurance-partition-key";
    private static final String SORT_KEY = "assurance-sort-key";

    public DynamoService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public String save(String id, String nome) {

        Map<String, AttributeValue> item = new HashMap<>();

        // Configura a chave composta obrigatória
        item.put(PARTITION_KEY, AttributeValue.builder().s(id).build());
        item.put(SORT_KEY, AttributeValue.builder().s(nome).build()); // Usando nome como Sort Key
        item.put("nome", AttributeValue.builder().s(nome).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);

        return "Item salvo com sucesso";
    }

    public Map<String, String> find(String id) {
        // ATENÇÃO: No DynamoDB, o GetItem clássico exige a chave completa (Partition + Sort).
        // Se você só tem o ID, o correto conceitualmente seria usar o método query().
        // Para o método find não quebrar, usei um mock/exemplo usando o ID no lugar da Sort Key.
        // Se a sua Sort Key for diferente, substitua o valor abaixo.
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PARTITION_KEY, AttributeValue.builder().s(id).build());
        key.put(SORT_KEY, AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();

        if (item == null || item.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>();
        item.forEach((k, v) -> {
            if (v.s() != null) result.put(k, v.s());
        });

        return result;
    }

    public String update(String id, String nome) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PARTITION_KEY, AttributeValue.builder().s(id).build());
        key.put(SORT_KEY, AttributeValue.builder().s(nome).build()); // A Sort Key faz parte da identificação do registro

        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("nome", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(nome).build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .attributeUpdates(updates)
                .build();

        dynamoDbClient.updateItem(request);

        return "Item atualizado com sucesso";
    }

    public String delete(String id) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PARTITION_KEY, AttributeValue.builder().s(id).build());
        key.put(SORT_KEY, AttributeValue.builder().s(id).build()); // Exige a Sort Key para exclusão pontual

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        dynamoDbClient.deleteItem(request);

        return "Item removido com sucesso";
    }

    public List<Map<String, String>> query(String id) {

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":assurance-partition-key", AttributeValue.builder().s(id).build());

        // A Query funciona perfeitamente buscando apenas pela Partition Key!
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("assurance-partition-key = :assurance-partition-key")
                .expressionAttributeValues(values)
                .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.query(request).items();

        return toStringList(items);
    }

    public List<Map<String, String>> scan() {

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.scan(request).items();

        return toStringList(items);
    }

    public List<Map<String, String>> batchGet(List<String> ids) {

        List<Map<String, AttributeValue>> keys = new ArrayList<>();
        for (String id : ids) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(PARTITION_KEY, AttributeValue.builder().s(id).build());
            key.put(SORT_KEY, AttributeValue.builder().s(id).build()); // Adicionado Sort Key obrigatória
            keys.add(key);
        }

        KeysAndAttributes keysAndAttributes = KeysAndAttributes.builder()
                .keys(keys)
                .build();

        BatchGetItemRequest request = BatchGetItemRequest.builder()
                .requestItems(Map.of(TABLE_NAME, keysAndAttributes))
                .build();

        List<Map<String, AttributeValue>> items =
                dynamoDbClient.batchGetItem(request).responses().get(TABLE_NAME);

        return toStringList(items);
    }

    public String batchWrite(List<Map<String, String>> itens) {

        List<WriteRequest> writeRequests = new ArrayList<>();

        for (Map<String, String> dados : itens) {
            Map<String, AttributeValue> item = new HashMap<>();

            // Garante que se o lote enviado não contiver as chaves explícitas estruturadas, o Dynamo vai falhar.
            dados.forEach((k, v) -> item.put(k, AttributeValue.builder().s(v).build()));

            writeRequests.add(WriteRequest.builder()
                    .putRequest(PutRequest.builder().item(item).build())
                    .build());
        }

        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
                .requestItems(Map.of(TABLE_NAME, writeRequests))
                .build();

        dynamoDbClient.batchWriteItem(request);

        return "Itens salvos em lote com sucesso";
    }

    private List<Map<String, String>> toStringList(List<Map<String, AttributeValue>> items) {

        List<Map<String, String>> result = new ArrayList<>();

        if (items == null) {
            return result;
        }

        for (Map<String, AttributeValue> item : items) {
            Map<String, String> converted = new HashMap<>();
            item.forEach((k, v) -> {
                if (v.s() != null) {
                    converted.put(k, v.s());
                }
            });
            result.add(converted);
        }

        return result;
    }
}