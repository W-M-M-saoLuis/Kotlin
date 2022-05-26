# Visão geral do provedor de agenda

 





O Provedor de agenda é um repositório para eventos da agenda do usuário. A API Calendar Provider permite consultar, inserir, atualizar e excluir operações em agendas, eventos, participantes, lembretes etc.

Essa API pode ser usada por aplicativos e adaptadores de sincronização. As regras variam conforme o tipo de programa que realiza as chamadas. Este documento se concentra principalmente no uso da API Calendar Provider como um aplicativo. Veja uma discussão sobre as diferenças entre adaptadores de sincronização em [Adaptadores de sincronização](https://developer.android.com/guide/topics/providers/calendar-provider#sync-adapter).

Normalmente, para ler ou programar dados da agenda, o manifesto de um aplicativo precisa conter as permissões adequadas descritas em [Permissões do usuário](https://developer.android.com/guide/topics/providers/calendar-provider#manifest). Para facilitar a realização de operações comuns, o Provedor de agenda fornece um conjunto de intents conforme descrito em [Intents do Agenda](https://developer.android.com/guide/topics/providers/calendar-provider#intents). Esses intents levam os usuários ao aplicativo Agenda para inserir, exibir e editar eventos. O usuário interage com o Agenda e, em seguida, retorna ao aplicativo original. Assim, o aplicativo não precisa solicitar permissões nem fornecer uma interface do usuário para exibir ou criar eventos.

## Conceitos básicos

Os [provedores de conteúdo](https://developer.android.com/guide/topics/providers/content-providers) armazenam dados que podem ser acessados por aplicativos. Os provedores de conteúdo oferecidos pela plataforma Android (inclusive o Provedor de agenda) normalmente expõem dados como um conjunto de tabelas baseadas em um modelo de banco de dados relacional, em que cada linha é um registro e cada coluna são dados de um tipo e significado específico. Por meio da API Calendar Provider, os aplicativos e adaptadores de sincronização podem ter acesso de leitura/gravação às tabelas do banco de dados que armazenam informações da agenda do usuário.

Cada provedor de conteúdo expõe um URI público (agrupado como um objeto `Uri`) que identifica exclusivamente o conjunto de dados. Um provedor de conteúdo que controla diversos conjuntos de dados (diversas tabelas) expõe um URI separado para cada um. Todos os URIs de provedores começam com a string “content://”. Isso identifica os dados como controlados por um provedor de conteúdo. O Provedor de agenda define constantes para os URIs de cada uma das classes (tabelas). Esses URIs têm o formato `*<class>*.CONTENT_URI`. Por exemplo: `Events.CONTENT_URI`.

A imagem 1 exibe uma representação gráfica do modelo de dados do Provedor de agenda. Ela ilustra as tabelas e os campos principais que as vinculam entre si.

![Modelo de dados do Provedor de agenda.](https://developer.android.com/images/providers/datamodel.png)

**Imagem 1.** Modelo de dados do Provedor de Agenda.

Cada usuário pode ter diversas agendas e associá-las a diferentes tipos de conta (Google Agenda, Exchange etc.).

O `CalendarContract` define o modelo de dados de informações relacionadas a eventos e agendas. Esses dados são armazenados em diversas tabelas, que são listadas a seguir.

| Tabela (classe)              | Descrição                                                    |
| :--------------------------- | :----------------------------------------------------------- |
| `CalendarContract.Calendars` | Essa tabela contém as informações específicas da agenda. Cada linha nessa tabela contém os detalhes de uma única agenda, como nome, cor, informações de sincronização etc. |
| `CalendarContract.Events`    | Essa tabela contém as informações específicas do evento. Cada linha nessa tabela tem as informações de um único evento. Por exemplo, título do evento, local, horário de início, horário de término etc. O evento pode ocorrer uma vez ou diversas vezes. Os participantes, lembretes e propriedades estendidas são armazenados em tabelas separadas. Cada um deles tem um `EVENT_ID` que referencia o `_ID` na tabela de eventos. |
| `CalendarContract.Instances` | Essa tabela contém os horários de início e término de cada ocorrência de um evento. Cada linha nessa tabela representa uma única ocorrência do evento. Para eventos de ocorrência única, há um mapeamento 1:1 de instâncias para eventos. Para eventos recorrentes, diversas linhas correspondentes a diversas ocorrências daquele evento são geradas automaticamente. |
| `CalendarContract.Attendees` | Essa tabela contém as informações dos participantes (convidados) do evento. Cada linha representa um único convidado de um evento. Ela especifica o tipo de convidado e a resposta quanto à participação do convidado no evento. |
| `CalendarContract.Reminders` | Essa tabela contém os dados de alerta/notificação. Cada linha representa um único alerta de um evento. Um evento pode ter vários lembretes. O número máximo de lembretes por evento é especificado em `MAX_REMINDERS`, que é definido pelo adaptador de sincronização que detém a agenda em questão. Os lembretes são especificados em minutos antes do evento e têm um método que determina a forma de alertar o usuário. |

A API Calendar Provider é projetada para ser flexível e poderosa. Ao mesmo tempo, é importante fornecer uma boa experiência ao usuário final e proteger a integridade da agenda e seus dados. Para isso, veja alguns pontos a considerar ao usar a API:

- **Inserção, atualização e exibição de eventos da agenda.** Para inserir, modificar e ler eventos diretamente do provedor de agenda, é necessário ter as [permissões](https://developer.android.com/guide/topics/providers/calendar-provider#manifest) apropriadas. Contudo, se o aplicativo em criação não for um aplicativo de agenda totalmente desenvolvido nem um adaptador de sincronização, não será necessário solicitar essas permissões. Em vez disso, é possível usar intents compatíveis com o aplicativo Agenda do Android para oferecer operações de leitura e gravação. Ao usar os intents, o aplicativo envia usuários ao aplicativo Agenda para realizar a operação desejada em um formulário pré-preenchido. Após finalizarem a operação, eles serão direcionados de volta ao aplicativo. Ao projetar seu aplicativo para realizar operações comuns por meio do Agenda, os usuários terão uma experiência em uma interface do usuário robusta e consistente. Essa é a abordagem recomendada. Para ver mais informações, consulte [Intents do Agenda](https://developer.android.com/guide/topics/providers/calendar-provider#intents).
- **Adaptadores de sincronização.** Os adaptadores de sincronização sincronizam os dados da agenda em um dispositivo do usuário com outro servidor ou fonte de dados. Nas tabelas `CalendarContract.Calendars` e `CalendarContract.Events`, há colunas reservadas para os adaptadores de sincronização usarem. O provedor e os aplicativos não devem modificá-las. De fato, elas não são visíveis, a menos que sejam acessadas como um adaptador de sincronização. Para ver mais informações sobre adaptadores de sincronização, consulte [Adaptadores de sincronização](https://developer.android.com/guide/topics/providers/calendar-provider#sync-adapter).

## Permissões do usuário

Para ler dados da agenda, o aplicativo deve conter a permissão `READ_CALENDAR` no arquivo de manifesto. Ele deve conter a permissão `WRITE_CALENDAR` para excluir, inserir ou atualizar dados da agenda:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"...>
    <uses-sdk android:minSdkVersion="14" />
    <uses-permission android:name="android.permission.READ_CALENDAR" />
    <uses-permission android:name="android.permission.WRITE_CALENDAR" />
    ...
</manifest>
```

## Tabela de agendas

A tabela `CalendarContract.Calendars` contém detalhes de agendas individuais. As colunas Agendas a seguir são graváveis tanto por aplicativos quanto por [adaptadores de sincronização](https://developer.android.com/guide/topics/providers/calendar-provider#sync-adapter). Para ver uma lista completa de campos compatíveis, consulte a referência de `CalendarContract.Calendars`.

| Constante               | Descrição                                                    |
| :---------------------- | :----------------------------------------------------------- |
| `NAME`                  | É o nome da agenda.                                          |
| `CALENDAR_DISPLAY_NAME` | É o nome dessa agenda que será exibido ao usuário.           |
| `VISIBLE`               | É um booleano indicando se a agenda foi selecionada para ser exibida. Um valor de 0 indica que eventos associados a essa agenda não devem ser exibidos. Um valor de 1 indica que eventos associados a essa agenda devem ser exibidos. Esse valor afeta a geração de linhas na tabela `CalendarContract.Instances`. |
| `SYNC_EVENTS`           | É o booleano que indica se a agenda deve ser sincronizada e armazenar os eventos no dispositivo. Um valor de 0 indica a não sincronização dessa agenda e o não armazenamento dos eventos no dispositivo. Um valor de 1 indica a sincronização dos eventos dessa agenda e o armazenamento deles no dispositivo. |

### Incluir um tipo de conta para todas as operações

Ao consultar um `Calendars.ACCOUNT_NAME`, é necessário incluir `Calendars.ACCOUNT_TYPE` na seleção. Isso porque determinada conta só é considerada exclusiva devido a seus `ACCOUNT_NAME` e `ACCOUNT_TYPE`. `ACCOUNT_TYPE` é a string correspondente ao autenticador da conta que foi usado quando ela foi registrada com o `AccountManager`. Há também um tipo especial de conta, `ACCOUNT_TYPE_LOCAL`, para agendas não associadas a nenhuma conta do dispositivo. As contas `ACCOUNT_TYPE_LOCAL` não são sincronizadas.

### Consultar a agenda

A seguir, há um exemplo que mostra como conseguir as agendas de propriedade de determinado usuário. Para simplificar o exemplo, a operação de consulta é exibida no thread de interface do usuário (“thread principal”). Na prática, isso deve ser feito em um thread assíncrono em vez de no thread principal. Para ver mais discussões, consulte [Carregadores](https://developer.android.com/guide/components/loaders). Se você não estiver somente lendo dados, mas fazendo modificações, consulte `AsyncQueryHandler`.

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
// Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
private val EVENT_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Calendars._ID,                     // 0
        CalendarContract.Calendars.ACCOUNT_NAME,            // 1
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
        CalendarContract.Calendars.OWNER_ACCOUNT            // 3
)

// The indices for the projection array above.
private const val PROJECTION_ID_INDEX: Int = 0
private const val PROJECTION_ACCOUNT_NAME_INDEX: In = 1
private const val PROJECTION_DISPLAY_NAME_INDEX: In = 2
private const val PROJECTION_OWNER_ACCOUNT_INDEX: In = 3
```

Na próxima parte do exemplo, você construirá a consulta. A seleção especifica os critérios da consulta. Neste exemplo, a consulta busca agendas que tenham o `ACCOUNT_NAME` “hera@example.com”, o `ACCOUNT_TYPE` “com.example” e o `OWNER_ACCOUNT` “hera@example.com”. Se você quiser ver todas as agendas visualizadas por um usuário, e não só os que ele possui, omita `OWNER_ACCOUNT`. Isso retornará um objeto `Cursor` que poderá ser usado para percorrer o conjunto de resultados retornado pela consulta do banco de dados. Para ver mais informações sobre o uso de consultas em provedores de conteúdo, leia [Provedores de conteúdo](https://developer.android.com/guide/topics/providers/content-providers).

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
// Run query
val uri: Uri = CalendarContract.Calendars.CONTENT_URI
val selection: String = "((${CalendarContract.Calendars.ACCOUNT_NAME} = ?) AND (" +
        "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?) AND (" +
        "${CalendarContract.Calendars.OWNER_ACCOUNT} = ?))"
val selectionArgs: Array<String> = arrayOf("hera@example.com", "com.example", "hera@example.com")
val cur: Cursor = contentResolver.query(uri, EVENT_PROJECTION, selection, selectionArgs, null)
```

Essa próxima seção usa o cursor para avançar pelo conjunto de resultados. Ele usa as constantes definidas no início do exemplo para retornar os valores de cada campo.

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
// Use the cursor to step through the returned records
while (cur.moveToNext()) {
    // Get the field values
    val calID: Long = cur.getLong(PROJECTION_ID_INDEX)
    val displayName: String = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
    val accountName: String = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
    val ownerName: String = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX)
    // Do something with the values...
}
```

### Modificar uma agenda

Para realizar uma atualização de uma agenda, é possível fornecer o `_ID` da agenda como um código anexado ao URI (`withAppendedId()`) ou como o primeiro item da seleção. A seleção deve iniciar com `"_id=?"` e o primeiro `selectionArg` deve ser o `_ID` da agenda. Também é possível realizar atualizações com a codificação no URI. Esse exemplo altera o nome de exibição de uma agenda usando a abordagem (`withAppendedId()`):

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
const val DEBUG_TAG: String = "MyActivity"
...
val calID: Long = 2
val values = ContentValues().apply {
    // The new display name for the calendar
    put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Trevor's Calendar")
}
val updateUri: Uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calID)
val rows: Int = contentResolver.update(updateUri, values, null, null)
Log.i(DEBUG_TAG, "Rows updated: $rows")
```

### Inserir uma agenda

As agendas são projetadas para serem gerenciadas principalmente por um adaptador de sincronização. Por isso, é preciso inserir novas agendas somente como um desses recursos. Para a maior parte, os aplicativos só podem efetuar mudanças superficiais em agendas, como mudar o nome de exibição. Se um aplicativo precisar criar uma agenda local, será possível inseri-la como um adaptador de sincronização usando um `ACCOUNT_TYPE` de `ACCOUNT_TYPE_LOCAL`. `ACCOUNT_TYPE_LOCAL` é um tipo de conta especial para agendas não associado a nenhuma conta do dispositivo. As agendas desse tipo não são sincronizadas com um servidor. Para ver discussões sobre adaptadores de sincronização, consulte [Adaptadores de sincronização](https://developer.android.com/guide/topics/providers/calendar-provider#sync-adapter).

## Tabela de eventos

A tabela `CalendarContract.Events` contém detalhes de eventos individuais. Para adicionar, atualizar ou excluir eventos, um aplicativo deve conter a permissão `WRITE_CALENDAR` [no arquivo de manifesto](https://developer.android.com/guide/topics/providers/calendar-provider#manifest).

As colunas Eventos a seguir são graváveis tanto por um aplicativo quanto por um adaptador de sincronização. Para ver uma lista completa de campos compatíveis, consulte a referência de `CalendarContract.Events`.

| Constante                  | Descrição                                                    |
| :------------------------- | :----------------------------------------------------------- |
| `CALENDAR_ID`              | É o `_ID` do evento na agenda.                               |
| `ORGANIZER`                | É o e-mail do organizador (proprietário) do evento.          |
| `TITLE`                    | É o título do evento.                                        |
| `EVENT_LOCATION`           | Indica onde o evento acontece.                               |
| `DESCRIPTION`              | É a descrição do evento.                                     |
| `DTSTART`                  | É o horário de início do evento em milissegundos em UTC desde a época. |
| `DTEND`                    | É o horário de término do evento em milissegundos em UTC desde a época. |
| `EVENT_TIMEZONE`           | É o fuso horário do evento.                                  |
| `EVENT_END_TIMEZONE`       | É o fuso horário do horário de término do evento.            |
| `DURATION`                 | É a duração do evento no formato [RFC5545](http://tools.ietf.org/html/rfc5545#section-3.8.2.5). Por exemplo: um valor de `"PT1H"` indica que o evento deve durar uma hora, e um valor de `"P2W"` mostra uma duração de 2 semanas. |
| `ALL_DAY`                  | Um valor de 1 indica que esse evento dura o dia inteiro, como definido pelo fuso horário local. Um valor de 0 indica que é um evento comum que pode iniciar e terminar a qualquer momento durante um dia. |
| `RRULE`                    | É a regra de recorrência do formato do evento. Por exemplo: `"FREQ=WEEKLY;COUNT=10;WKST=SU"`. Veja mais exemplos [aqui](http://tools.ietf.org/html/rfc5545#section-3.8.5.3). |
| `RDATE`                    | Mostra as datas de recorrência do evento. Normalmente, é possível usar `RDATE` com `RRULE` para definir um conjunto agregado de ocorrências repetidas. Para ver mais discussões, consulte a [Especificação RFC5545](http://tools.ietf.org/html/rfc5545#section-3.8.5.2). |
| `AVAILABILITY`             | Indica se esse evento é considerado como tempo ocupado ou como tempo livre que poderá ser reagendado. |
| `GUESTS_CAN_MODIFY`        | Indica se os convidados podem modificar o evento.            |
| `GUESTS_CAN_INVITE_OTHERS` | Define se os convidados podem convidar outras pessoas.       |
| `GUESTS_CAN_SEE_GUESTS`    | Indica se os convidados podem ver a lista de participantes.  |

### Adicionar eventos

Quando o aplicativo inserir um novo evento, use um intent `INSERT`, como descrito em [Uso de um intent para inserir um evento](https://developer.android.com/guide/topics/providers/calendar-provider#intent-insert). Contudo, caso necessário, é possível inserir eventos diretamente. Esta seção mostra como fazer isso.

Veja as regras para inserção de um novo evento:

- É preciso incluir `CALENDAR_ID` e `DTSTART`.
- É necessário incluir um `EVENT_TIMEZONE`. Para ter uma lista dos códigos de fuso horário instalados do sistema, use `getAvailableIDs()`. Essa regra não se aplica a inserções de evento pelo intent `INSERT`, processo descrito em [Uso de um intent para inserir um evento](https://developer.android.com/guide/topics/providers/calendar-provider#intent-insert). Nesses casos, é fornecido um fuso horário padrão.
- Para eventos não recorrentes, é preciso incluir `DTEND`.
- Para eventos recorrentes, inclua uma `DURATION` além da `RRULE` ou da `RDATE`. Essa regra não se aplica a inserções de evento pelo intent `INSERT`, descrita em [Uso de um intent para inserir um evento](https://developer.android.com/guide/topics/providers/calendar-provider#intent-insert). Nessa situação, é possível usar uma `RRULE` com `DTSTART` e `DTEND`. Dessa forma, o aplicativo Agenda a converte automaticamente em uma duração.

Veja um exemplo de inserção de um evento: Para simplificar, isso está sendo realizado no thread de IU. Na prática, inserções e atualizações devem ser feitas em um thread assíncrono para mover a ação para um thread de segundo plano. Para saber mais, consulte `AsyncQueryHandler`.

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val calID: Long = 3
val startMillis: Long = Calendar.getInstance().run {
    set(2012, 9, 14, 7, 30)
    timeInMillis
}
val endMillis: Long = Calendar.getInstance().run {
    set(2012, 9, 14, 8, 45)
    timeInMillis
}
...

val values = ContentValues().apply {
    put(CalendarContract.Events.DTSTART, startMillis)
    put(CalendarContract.Events.DTEND, endMillis)
    put(CalendarContract.Events.TITLE, "Jazzercise")
    put(CalendarContract.Events.DESCRIPTION, "Group workout")
    put(CalendarContract.Events.CALENDAR_ID, calID)
    put(CalendarContract.Events.EVENT_TIMEZONE, "America/Los_Angeles")
}
val uri: Uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

// get the event ID that is the last element in the Uri
val eventID: Long = uri.lastPathSegment.toLong()
//
// ... do something with event ID
//
//
```

**Observação:** veja como esse exemplo captura o código do evento depois que ele é criado. Esse é o modo mais fácil de conseguir um código de evento. Muitas vezes, o código do evento é necessário para realizar outras operações de agenda. Por exemplo, adicionar participantes ou lembretes a um evento.

### Atualizar eventos

Quando o aplicativo quiser permitir que o usuário edite um evento, é recomendável usar um intent `EDIT`, como descrito em [Uso de um intent para editar um evento](https://developer.android.com/guide/topics/providers/calendar-provider#intent-edit). Contudo, caso necessário, será possível editar eventos diretamente. Para realizar uma atualização de um evento, é possível fornecer o `_ID` do evento como um código anexado ao URI `withAppendedId()` ou como o primeiro item de seleção. A seleção deve iniciar com `"_id=?"` e o primeiro `selectionArg` deve ser o `_ID` do evento. Você também pode realizar atualizações usando uma seleção sem código. Veja um exemplo de atualização de um evento. É possível mudar o título do evento usando a abordagem `withAppendedId()`:

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val DEBUG_TAG = "MyActivity"
...
val eventID: Long = 188
...
val values = ContentValues().apply {
    // The new title for the event
    put(CalendarContract.Events.TITLE, "Kickboxing")
}
val updateUri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID)
val rows: Int = contentResolver.update(updateUri, values, null, null)
Log.i(DEBUG_TAG, "Rows updated: $rows")
```

### Excluir eventos

É possível excluir um evento pelo `_ID` como um código anexado ao URI ou usando a seleção padrão. Se você usar um código anexado, também não poderá fazer uma seleção. Existem duas versões de exclusão: como um aplicativo e como um adaptador de sincronização. A exclusão do aplicativo define a coluna *deleted* como 1. Esse sinalizador diz ao adaptador de sincronização que a linha foi excluída e que essa exclusão deve ser propagada para o servidor. A exclusão por um adaptador de sincronização remove o evento do banco de dados junto com todos os dados associados. Veja o exemplo de um aplicativo que exclui um evento pelo `_ID`:

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val DEBUG_TAG = "MyActivity"
...
val eventID: Long = 201
...
val deleteUri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID)
val rows: Int = contentResolver.delete(deleteUri, null, null)
Log.i(DEBUG_TAG, "Rows deleted: $rows")
```

## Tabela de participantes

Cada linha da tabela `CalendarContract.Attendees` representa um único participante ou convidado de um evento. Chamar `query()` retorna uma lista de participantes para o evento com o `EVENT_ID` fornecido. Esse `EVENT_ID` deve corresponder ao `_ID` de determinado evento.

A tabela a seguir lista os campos graváveis. Ao inserir um novo participante, será necessário incluir todos eles, exceto `ATTENDEE_NAME`.

| Constante               | Descrição                                                    |
| :---------------------- | :----------------------------------------------------------- |
| `EVENT_ID`              | É o código do evento.                                        |
| `ATTENDEE_NAME`         | É o nome do participante.                                    |
| `ATTENDEE_EMAIL`        | É o endereço de e-mail do participante.                      |
| `ATTENDEE_RELATIONSHIP` | É a relação do participante com o evento. Pode ser:`RELATIONSHIP_ATTENDEE``RELATIONSHIP_NONE``RELATIONSHIP_ORGANIZER``RELATIONSHIP_PERFORMER``RELATIONSHIP_SPEAKER` |
| `ATTENDEE_TYPE`         | Indica o tipo de participante. Pode ser:`TYPE_REQUIRED``TYPE_OPTIONAL` |
| `ATTENDEE_STATUS`       | É o status de participação do participante. Pode ser:`ATTENDEE_STATUS_ACCEPTED``ATTENDEE_STATUS_DECLINED``ATTENDEE_STATUS_INVITED``ATTENDEE_STATUS_NONE``ATTENDEE_STATUS_TENTATIVE` |

### Adicionar participantes

A seguir, há um exemplo que adiciona um único participante a um evento. `EVENT_ID` é obrigatório:

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val eventID: Long = 202
...
val values = ContentValues().apply {
    put(CalendarContract.Attendees.ATTENDEE_NAME, "Trevor")
    put(CalendarContract.Attendees.ATTENDEE_EMAIL, "trevor@example.com")
    put(
        CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
        CalendarContract.Attendees.RELATIONSHIP_ATTENDEE
    )
    put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_OPTIONAL)
    put(
        CalendarContract.Attendees.ATTENDEE_STATUS,
        CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
    )
    put(CalendarContract.Attendees.EVENT_ID, eventID)
}
val uri: Uri = contentResolver.insert(CalendarContract.Attendees.CONTENT_URI, values)
```

## Tabela de lembretes

Cada linha da tabela `CalendarContract.Reminders` representa um único lembrete de um evento. Chamar `query()` retornará uma lista de lembretes para o evento com o `EVENT_ID` em questão.

A tabela a seguir relaciona os campos graváveis de lembretes. Todos eles devem ser incluídos ao inserir um novo lembrete. Os adaptadores de sincronização especificam os tipos de lembretes compatíveis na tabela `CalendarContract.Calendars`. Consulte `ALLOWED_REMINDERS` para ver mais detalhes.

| Constante  | Descrição                                                    |
| :--------- | :----------------------------------------------------------- |
| `EVENT_ID` | É o código do evento.                                        |
| `MINUTES`  | Indica os minutos antes do evento para o acionamento do lembrete. |
| `METHOD`   | É o método de alarme, como definido no servidor. Pode ser:`METHOD_ALERT``METHOD_DEFAULT``METHOD_EMAIL``METHOD_SMS` |

### Adicionar lembretes

Este exemplo adiciona um lembrete para um evento. O lembrete é acionado 15 minutos antes do evento.

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val eventID: Long = 221
...
val values = ContentValues().apply {
    put(CalendarContract.Reminders.MINUTES, 15)
    put(CalendarContract.Reminders.EVENT_ID, eventID)
    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
}
val uri: Uri = contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
```

## Tabela de instâncias

A tabela `CalendarContract.Instances` contém os horários de início e término das ocorrências de um evento. Cada linha nessa tabela representa uma única ocorrência do evento. A tabela de instâncias não é gravável e fornece somente um modo de consultar ocorrências de eventos.

A tabela a seguir relaciona alguns dos campos passíveis de consulta de uma instância. O fuso horário é definido por `KEY_TIMEZONE_TYPE` e `KEY_TIMEZONE_INSTANCES`.

| Constante      | Descrição                                                    |
| :------------- | :----------------------------------------------------------- |
| `BEGIN`        | É o horário de início da instância, em milissegundos UTC.    |
| `END`          | É o horário de término da instância, em milissegundos UTC.   |
| `END_DAY`      | Indica o dia final do calendário juliano da instância, relativo ao fuso horário do Agenda. |
| `END_MINUTE`   | É o minuto final da instância calculado a partir de meia-noite no fuso horário do Agenda. |
| `EVENT_ID`     | É o `_ID` do evento para essa instância.                     |
| `START_DAY`    | É o dia inicial do calendário juliano da instância, relativo ao fuso horário do Agenda. |
| `START_MINUTE` | É o minuto inicial da instância calculado a partir de meia-noite, relativo ao fuso horário do Agenda. |

### Consultar a tabela de instâncias

Para consultar a tabela de instâncias, é necessário especificar um intervalo de tempo para a consulta no URI. Neste exemplo, `CalendarContract.Instances` recebe acesso ao campo `TITLE` por meio da implementação da interface `CalendarContract.EventsColumns`. Em outras palavras, `TITLE` é retornado por uma exibição do banco de dados, não pela consulta da tabela `CalendarContract.Instances` bruta.

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
const val DEBUG_TAG: String = "MyActivity"
val INSTANCE_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Instances.EVENT_ID, // 0
        CalendarContract.Instances.BEGIN, // 1
        CalendarContract.Instances.TITLE // 2
)

// The indices for the projection array above.
const val PROJECTION_ID_INDEX: Int = 0
const val PROJECTION_BEGIN_INDEX: Int = 1
const val PROJECTION_TITLE_INDEX: Int = 2

// Specify the date range you want to search for recurring
// event instances
val startMillis: Long = Calendar.getInstance().run {
    set(2011, 9, 23, 8, 0)
    timeInMillis
}
val endMillis: Long = Calendar.getInstance().run {
    set(2011, 10, 24, 8, 0)
    timeInMillis
}

// The ID of the recurring event whose instances you are searching
// for in the Instances table
val selection: String = "${CalendarContract.Instances.EVENT_ID} = ?"
val selectionArgs: Array<String> = arrayOf("207")

// Construct the query with the desired date range.
val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
ContentUris.appendId(builder, startMillis)
ContentUris.appendId(builder, endMillis)

// Submit the query
val cur: Cursor = contentResolver.query(
        builder.build(),
        INSTANCE_PROJECTION,
        selection,
        selectionArgs, null
)
while (cur.moveToNext()) {
    // Get the field values
    val eventID: Long = cur.getLong(PROJECTION_ID_INDEX)
    val beginVal: Long = cur.getLong(PROJECTION_BEGIN_INDEX)
    val title: String = cur.getString(PROJECTION_TITLE_INDEX)

    // Do something with the values.
    Log.i(DEBUG_TAG, "Event: $title")
    val calendar = Calendar.getInstance().apply {
        timeInMillis = beginVal
    }
    val formatter = SimpleDateFormat("MM/dd/yyyy")
    Log.i(DEBUG_TAG, "Date: ${formatter.format(calendar.time)}")
}
```

## Intents do Agenda

O aplicativo não precisa de [permissões](https://developer.android.com/guide/topics/providers/calendar-provider#manifest) para ler e gravar dados de agenda. Em vez disso, ele pode usar intents compatíveis com o aplicativo Agenda do Android para entregar operações de leitura e gravação. A tabela a seguir lista os intents compatíveis com o Provedor de Agenda.

| Ação             | URI                                                          | Descrição                                                    | Extras                                                       |
| :--------------- | :----------------------------------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| `VIEW`           | `content://com.android.calendar/time/<ms_since_epoch>`Também é possível consultar o URI com `CalendarContract.CONTENT_URI`. Para ver um exemplo do uso desse intent, consulte [Uso de intents para exibir dados de agendas](https://developer.android.com/guide/topics/providers/calendar-provider#intent-view). | Abre a agenda no horário especificado por `<ms_since_epoch>`. | Nenhum.                                                      |
| `VIEW`           | `content://com.android.calendar/events/<event_id>`Também é possível consultar o URI com `Events.CONTENT_URI`. Para ver um exemplo do uso desse intent, consulte [Uso de intents para exibir dados de agendas](https://developer.android.com/guide/topics/providers/calendar-provider#intent-view). | Exibe o evento especificado por `<event_id>`.                | `CalendarContract.EXTRA_EVENT_BEGIN_TIME`   `CalendarContract.EXTRA_EVENT_END_TIME` |
| `EDIT`           | `content://com.android.calendar/events/<event_id>`Também é possível consultar o URI com `Events.CONTENT_URI`. Para ver um exemplo do uso desse intent, consulte [Uso de um intent para editar um evento](https://developer.android.com/guide/topics/providers/calendar-provider#intent-edit). | Edita o evento especificado por `<event_id>`.                | `CalendarContract.EXTRA_EVENT_BEGIN_TIME`   `CalendarContract.EXTRA_EVENT_END_TIME` |
| `EDIT`  `INSERT` | `content://com.android.calendar/events`Também é possível consultar o URI com `Events.CONTENT_URI`. Para ver um exemplo do uso desse intent, consulte [Uso de um intent para inserir um evento](https://developer.android.com/guide/topics/providers/calendar-provider#intent-insert). | Crie um evento.                                              | Qualquer um dos extras listados na tabela abaixo.            |

A tabela a seguir lista os extras de intenção compatíveis com o Provedor de Agenda:

| Extra de intent                           | Descrição                                                    |
| :---------------------------------------- | :----------------------------------------------------------- |
| `Events.TITLE`                            | É o nome do evento.                                          |
| `CalendarContract.EXTRA_EVENT_BEGIN_TIME` | É o horário de início do evento em milissegundos a partir da época. |
| `CalendarContract.EXTRA_EVENT_END_TIME`   | É o horário de término do evento em milissegundos a partir da época. |
| `CalendarContract.EXTRA_EVENT_ALL_DAY`    | É um booleano que indica que um evento acontece o dia inteiro. O valor pode ser `true` ou `false`. |
| `Events.EVENT_LOCATION`                   | Indica o local do evento.                                    |
| `Events.DESCRIPTION`                      | É a descrição do evento.                                     |
| `Intent.EXTRA_EMAIL`                      | São os endereços de e-mail das pessoas que serão convidadas em forma de lista, com termos separados por vírgula. |
| `Events.RRULE`                            | É a regra de recorrência do evento.                          |
| `Events.ACCESS_LEVEL`                     | Indica se o evento é privado ou público.                     |
| `Events.AVAILABILITY`                     | Indica se esse evento é considerado como tempo ocupado ou como tempo livre que poderá ser reagendado. |

As seções a seguir descrevem como usar estes intents.

### Usar um intent para inserir um evento

O uso do intent `INSERT` permite que seu aplicativo entregue a tarefa de inserção de eventos ao próprio Agenda. Com esta abordagem, seu aplicativo não precisará incluir a permissão `WRITE_CALENDAR` no [arquivo de manifesto](https://developer.android.com/guide/topics/providers/calendar-provider#manifest).

Quando usuários executam um aplicativo que usa essa abordagem, ele os direciona ao Agenda para finalizar a adição do evento. O intent `INSERT` usa campos extras para pré-preencher um formulário com os detalhes do evento no Agenda. Assim, os usuários poderão cancelar o evento, editar o formulário conforme necessário ou salvar o evento na sua agenda.

A seguir, há um snippet de código que agenda um evento em 19 de janeiro de 2012, que acontece das 7h30 às 8h30. Observe o seguinte sobre esse snippet de código:

- Ele especifica `Events.CONTENT_URI` como o URI.
- Ele usa os campos extras `CalendarContract.EXTRA_EVENT_BEGIN_TIME` `CalendarContract.EXTRA_EVENT_END_TIME` para pré-preencher o formulário com o horário do evento. Os valores desses horários devem estar em milissegundos UTC a partir da época.
- Ele usa o campo extra `Intent.EXTRA_EMAIL` para fornecer uma lista de termos separados por vírgula de convidados, especificados por endereço de e-mail.

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val startMillis: Long = Calendar.getInstance().run {
    set(2012, 0, 19, 7, 30)
    timeInMillis
}
val endMillis: Long = Calendar.getInstance().run {
    set(2012, 0, 19, 8, 30)
    timeInMillis
}
val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        .putExtra(CalendarContract.Events.TITLE, "Yoga")
        .putExtra(CalendarContract.Events.DESCRIPTION, "Group class")
        .putExtra(CalendarContract.Events.EVENT_LOCATION, "The gym")
        .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        .putExtra(Intent.EXTRA_EMAIL, "rowan@example.com,trevor@example.com")
startActivity(intent)
```

### Usar um intent para editar um evento

É possível atualizar um evento diretamente, como descrito em [Atualização de eventos](https://developer.android.com/guide/topics/providers/calendar-provider#update-event). Porém, o uso do intent `EDIT` permite que um aplicativo sem permissão entregue a edição de eventos ao Agenda. Quando os usuários terminarem a edição do evento no Agenda, eles serão retornados ao aplicativo original.

A seguir há um exemplo de um intent que define um novo título para o evento especificado e permite aos usuários editar o evento no Agenda.

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val eventID: Long = 208
val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID)
val intent = Intent(Intent.ACTION_EDIT)
        .setData(uri)
        .putExtra(CalendarContract.Events.TITLE, "My New Title")
startActivity(intent)
```

### Usar intents para exibir dados de agenda

O Provedor de agenda oferece dois modos diferentes de usar o intent `VIEW`:

- Para abrir o Agenda em uma data específica.
- Para exibir um evento.

Veja um exemplo que mostra como abrir o Agenda em uma data específica:

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val startMillis: Long
...
val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()
        .appendPath("time")
ContentUris.appendId(builder, startMillis)
val intent = Intent(Intent.ACTION_VIEW)
        .setData(builder.build())
startActivity(intent)
```

Veja um exemplo que mostra como abrir um evento para exibição:

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
val eventID: Long = 208
...
val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID)
val intent = Intent(Intent.ACTION_VIEW).setData(uri)
startActivity(intent)
```

## Adaptadores de sincronização

Há pequenas diferenças somente nos modos de acesso ao Provedor de agenda via aplicativo e via adaptador de sincronização:

- Um adaptador de sincronização precisa especificar que é um adaptador de sincronização definindo `CALLER_IS_SYNCADAPTER` como `true`.
- Os adaptadores de sincronização precisam fornecer um `ACCOUNT_NAME` e um `ACCOUNT_TYPE` como parâmetros da consulta no URI.
- Os adaptadores de sincronização têm acesso de gravação a mais colunas do que um aplicativo ou widget. Por exemplo, um aplicativo só poderá modificar algumas características de uma agenda, como nome, nome de exibição, configurações de visibilidade e se a agenda está sincronizada. Por comparação, um adaptador de sincronização pode acessar não somente essas colunas, mas muitas outras, como cores da agenda, fuso horário, nível de acesso, local etc. No entanto, um adaptador de sincronização é restrito ao `ACCOUNT_NAME` e `ACCOUNT_TYPE` que especificou.

Veja um método auxiliar que pode ser usado para retornar um URI para uso com um adaptador de sincronização:

[Kotlin](https://developer.android.com/guide/topics/providers/calendar-provider#kotlin)[Java](https://developer.android.com/guide/topics/providers/calendar-provider#java)

```kotlin
fun asSyncAdapter(uri: Uri, account: String, accountType: String): Uri {
    return uri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType).build()
}
```

Fonte: https://developer.android.com/guide/topics/providers/calendar-provider 