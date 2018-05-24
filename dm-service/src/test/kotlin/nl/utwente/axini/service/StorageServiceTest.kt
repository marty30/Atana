package nl.utwente.axini.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import nl.utwente.axini.atana.models.TestRun
import org.junit.Test
import weka.core.Instances
import weka.core.converters.ArffLoader
import java.io.StringReader

class StorageServiceTest {

	val storageService = StorageService()

	@Test
	fun `check the creation of an Arff file`() {
		val testRun = jacksonObjectMapper().readValue<TestRun>("""
{
  "test_run_id": "9b8c2b5c-4490-456e-b299-59c28d78d421",
  "test_cases": [
    {
      "index": 1,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: No response received from SUT.",
      "steps": [
        {
          "label": {
            "name": "C220_SCRP_READY",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.614+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": 1,
          "advance_duration_ms": 1,
          "physical_label": "MjIwIFNDUlAgU2VydmljZSByZWFkeQ==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.622+0200",
          "notes": [

          ],
          "step_number": 1,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "R0VUIENTX1NJR04=\n",
          "label_parameters": {
            "get_variable_name": "CS_SIGN"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.628+0200",
          "notes": [

          ],
          "step_number": 2,
          "state_vector_size": 1,
          "advance_duration_ms": 2,
          "physical_label": "MjEwIENTX1NJR046U1NfT0ZG\n",
          "label_parameters": {
            "return_variable_name": "CS_SIGN",
            "variable_value": "SS_OFF"
          }
        },
        {
          "label": {
            "name": "RESUME",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.632+0200",
          "notes": [

          ],
          "step_number": 3,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "UkVTVU1F\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C201_RESUMED_OPERATION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.636+0200",
          "notes": [

          ],
          "step_number": 4,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjAxIFJlc3VtZWQgb3BlcmF0aW9u\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.639+0200",
          "notes": [

          ],
          "step_number": 5,
          "state_vector_size": 1,
          "advance_duration_ms": 1,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.647+0200",
          "notes": [

          ],
          "step_number": 6,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.650+0200",
          "notes": [

          ],
          "step_number": 7,
          "state_vector_size": 1,
          "advance_duration_ms": 0,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.655+0200",
          "notes": [

          ],
          "step_number": 8,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "RESUME",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.661+0200",
          "notes": [

          ],
          "step_number": 9,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "UkVTVU1F\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C201_RESUMED_OPERATION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.668+0200",
          "notes": [

          ],
          "step_number": 10,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "MjAxIFJlc3VtZWQgb3BlcmF0aW9u\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.681+0200",
          "notes": [

          ],
          "step_number": 11,
          "state_vector_size": 1,
          "advance_duration_ms": 1,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.689+0200",
          "notes": [

          ],
          "step_number": 12,
          "state_vector_size": 2,
          "advance_duration_ms": 7,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "RESETCR",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.699+0200",
          "notes": [

          ],
          "step_number": 13,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "UkVTRVRDUg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C501_SYNTAX_ERROR",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.706+0200",
          "notes": [

          ],
          "step_number": 14,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "NTAxIFN5bnRheCBlcnJvcg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "RESETCR",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.710+0200",
          "notes": [

          ],
          "step_number": 15,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "UkVTRVRDUg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C501_SYNTAX_ERROR",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.717+0200",
          "notes": [

          ],
          "step_number": 16,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "NTAxIFN5bnRheCBlcnJvcg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.721+0200",
          "notes": [

          ],
          "step_number": 17,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "R0VUIENTX1NJR04=\n",
          "label_parameters": {
            "get_variable_name": "CS_SIGN"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.729+0200",
          "notes": [

          ],
          "step_number": 18,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjEwIENTX1NJR046U1NfT0ZG\n",
          "label_parameters": {
            "return_variable_name": "CS_SIGN",
            "variable_value": "SS_OFF"
          }
        },
        {
          "label": {
            "name": "RESUME",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.734+0200",
          "notes": [

          ],
          "step_number": 19,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "UkVTVU1F\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C201_RESUMED_OPERATION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.739+0200",
          "notes": [

          ],
          "step_number": 20,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjAxIFJlc3VtZWQgb3BlcmF0aW9u\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.744+0200",
          "notes": [

          ],
          "step_number": 21,
          "state_vector_size": 1,
          "advance_duration_ms": 2,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.753+0200",
          "notes": [

          ],
          "step_number": 22,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.758+0200",
          "notes": [

          ],
          "step_number": 23,
          "state_vector_size": 1,
          "advance_duration_ms": 1,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.766+0200",
          "notes": [

          ],
          "step_number": 24,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "RESETCR",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.771+0200",
          "notes": [

          ],
          "step_number": 25,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "UkVTRVRDUg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C202_CASH_REGISTER_RESTORED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.777+0200",
          "notes": [

          ],
          "step_number": 26,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjAyIENhc2ggUmVnaXN0ZXIgcmVzdG9yZWQ=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "SIGNON_EXIST",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.781+0200",
          "notes": [

          ],
          "step_number": 27,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "U0lHTk9OIDg6MDE=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C251_SIGNED_ON",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.788+0200",
          "notes": [

          ],
          "step_number": 28,
          "state_vector_size": 1,
          "advance_duration_ms": 1,
          "physical_label": "MjUxIFNpZ25lZCBPbg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.792+0200",
          "notes": [

          ],
          "step_number": 29,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "R0VUIENTX0FDQ05U\n",
          "label_parameters": {
            "get_variable_name": "CS_ACCNT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.803+0200",
          "notes": [

          ],
          "step_number": 30,
          "state_vector_size": 1,
          "advance_duration_ms": 4,
          "physical_label": "MjEwIENTX0FDQ05UOjE6QVNfSURMRQ==\n",
          "label_parameters": {
            "return_variable_name": "CS_ACCNT",
            "variable_value": "AS_IDLE"
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.813+0200",
          "notes": [

          ],
          "step_number": 31,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "R0VUIENTX1NJR04=\n",
          "label_parameters": {
            "get_variable_name": "CS_SIGN"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.822+0200",
          "notes": [

          ],
          "step_number": 32,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjEwIENTX1NJR046U1NfT04=\n",
          "label_parameters": {
            "return_variable_name": "CS_SIGN",
            "variable_value": "SS_ON"
          }
        },
        {
          "label": {
            "name": "SIGNOFF",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.830+0200",
          "notes": [

          ],
          "step_number": 33,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "U0lHTk9GRg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C250_SIGNED_OFF",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.836+0200",
          "notes": [

          ],
          "step_number": 34,
          "state_vector_size": 1,
          "advance_duration_ms": 2,
          "physical_label": "MjUwIFNpZ25lZCBPZmY=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "RESUME",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.842+0200",
          "notes": [

          ],
          "step_number": 35,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "UkVTVU1F\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C201_RESUMED_OPERATION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.890+0200",
          "notes": [

          ],
          "step_number": 36,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjAxIFJlc3VtZWQgb3BlcmF0aW9u\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "RESETCR",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.895+0200",
          "notes": [

          ],
          "step_number": 37,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "UkVTRVRDUg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C202_CASH_REGISTER_RESTORED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.903+0200",
          "notes": [

          ],
          "step_number": 38,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjAyIENhc2ggUmVnaXN0ZXIgcmVzdG9yZWQ=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "RESETCR",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.908+0200",
          "notes": [

          ],
          "step_number": 39,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "UkVTRVRDUg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C202_CASH_REGISTER_RESTORED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.914+0200",
          "notes": [

          ],
          "step_number": 40,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjAyIENhc2ggUmVnaXN0ZXIgcmVzdG9yZWQ=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "RESUME",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.919+0200",
          "notes": [

          ],
          "step_number": 41,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "UkVTVU1F\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C201_RESUMED_OPERATION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.926+0200",
          "notes": [

          ],
          "step_number": 42,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjAxIFJlc3VtZWQgb3BlcmF0aW9u\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.930+0200",
          "notes": [

          ],
          "step_number": 43,
          "state_vector_size": 1,
          "advance_duration_ms": 1,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.938+0200",
          "notes": [

          ],
          "step_number": 44,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "RESUME",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.942+0200",
          "notes": [

          ],
          "step_number": 45,
          "state_vector_size": 2,
          "advance_duration_ms": 7,
          "physical_label": "UkVTVU1F\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C201_RESUMED_OPERATION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.953+0200",
          "notes": [

          ],
          "step_number": 46,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjAxIFJlc3VtZWQgb3BlcmF0aW9u\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "SIGNON_EXIST",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.957+0200",
          "notes": [

          ],
          "step_number": 47,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "U0lHTk9OIDg6MDE=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C251_SIGNED_ON",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.964+0200",
          "notes": [

          ],
          "step_number": 48,
          "state_vector_size": 1,
          "advance_duration_ms": 0,
          "physical_label": "MjUxIFNpZ25lZCBPbg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.967+0200",
          "notes": [

          ],
          "step_number": 49,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "R0VUIENTX0FDQ05U\n",
          "label_parameters": {
            "get_variable_name": "CS_ACCNT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.978+0200",
          "notes": [

          ],
          "step_number": 50,
          "state_vector_size": 1,
          "advance_duration_ms": 3,
          "physical_label": "MjEwIENTX0FDQ05UOjE6QVNfSURMRQ==\n",
          "label_parameters": {
            "return_variable_name": "CS_ACCNT",
            "variable_value": "AS_IDLE"
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.987+0200",
          "notes": [

          ],
          "step_number": 51,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "R0VUIEFJX1NUQU1Q\n",
          "label_parameters": {
            "get_variable_name": "AI_STAMP"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:36.995+0200",
          "notes": [

          ],
          "step_number": 52,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "MjEwIEFJX1NUQU1QOjA6MCwwMA==\n",
          "label_parameters": {
            "return_variable_name": "AI_STAMP"
          }
        },
        {
          "label": {
            "name": "PRINT_NEXIST",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.005+0200",
          "notes": [

          ],
          "step_number": 53,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "UFJJTlQgMDIzMTU2OjxiPllvdXIgdGV4dCBoZXJlPC9iPg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C260_DATA_PRINTED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.014+0200",
          "notes": [

          ],
          "step_number": 54,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjYwIERhdGEgcHJpbnRlZA==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "RESUME",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.023+0200",
          "notes": [

          ],
          "step_number": 55,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "UkVTVU1F\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C201_RESUMED_OPERATION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.029+0200",
          "notes": [

          ],
          "step_number": 56,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjAxIFJlc3VtZWQgb3BlcmF0aW9u\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.036+0200",
          "notes": [

          ],
          "step_number": 57,
          "state_vector_size": 1,
          "advance_duration_ms": 2,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.045+0200",
          "notes": [

          ],
          "step_number": 58,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "ARTID_WEIGHTED",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.054+0200",
          "notes": [

          ],
          "step_number": 59,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "QVJUSUQgMjAwMTMyMjc=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C213_ART_TEXT",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.061+0200",
          "notes": [

          ],
          "step_number": 60,
          "state_vector_size": 3,
          "advance_duration_ms": 2,
          "physical_label": "MjEzIGRlc2M9IkFsbWEiIHByaWNlPTkxLDk5IHdndA==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "SIGNOFF",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.068+0200",
          "notes": [

          ],
          "step_number": 61,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "U0lHTk9GRg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C250_SIGNED_OFF",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.074+0200",
          "notes": [

          ],
          "step_number": 62,
          "state_vector_size": 1,
          "advance_duration_ms": 2,
          "physical_label": "MjUwIFNpZ25lZCBPZmY=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.079+0200",
          "notes": [

          ],
          "step_number": 63,
          "state_vector_size": 1,
          "advance_duration_ms": 0,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.086+0200",
          "notes": [

          ],
          "step_number": 64,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.091+0200",
          "notes": [

          ],
          "step_number": 65,
          "state_vector_size": 1,
          "advance_duration_ms": 2,
          "physical_label": "R0VUIENTX0VWRU5U\n",
          "label_parameters": {
            "get_variable_name": "CS_EVENT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.098+0200",
          "notes": [

          ],
          "step_number": 66,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjEwIENTX0VWRU5UOkVWX05PTkU=\n",
          "label_parameters": {
            "return_variable_name": "CS_EVENT",
            "variable_value": "EV_NONE"
          }
        },
        {
          "label": {
            "name": "SIGNON_EXIST",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.104+0200",
          "notes": [

          ],
          "step_number": 67,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "U0lHTk9OIDg6MDE=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C251_SIGNED_ON",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.110+0200",
          "notes": [

          ],
          "step_number": 68,
          "state_vector_size": 1,
          "advance_duration_ms": 1,
          "physical_label": "MjUxIFNpZ25lZCBPbg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.116+0200",
          "notes": [

          ],
          "step_number": 69,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "R0VUIENTX0FDQ05U\n",
          "label_parameters": {
            "get_variable_name": "CS_ACCNT"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.122+0200",
          "notes": [

          ],
          "step_number": 70,
          "state_vector_size": 1,
          "advance_duration_ms": 2,
          "physical_label": "MjEwIENTX0FDQ05UOjE6QVNfSURMRQ==\n",
          "label_parameters": {
            "return_variable_name": "CS_ACCNT",
            "variable_value": "AS_IDLE"
          }
        },
        {
          "label": {
            "name": "RESETCR",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.129+0200",
          "notes": [

          ],
          "step_number": 71,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "UkVTRVRDUg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C202_CASH_REGISTER_RESTORED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.134+0200",
          "notes": [

          ],
          "step_number": 72,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjAyIENhc2ggUmVnaXN0ZXIgcmVzdG9yZWQ=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "RESETCR",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.142+0200",
          "notes": [

          ],
          "step_number": 73,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "UkVTRVRDUg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C202_CASH_REGISTER_RESTORED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.148+0200",
          "notes": [

          ],
          "step_number": 74,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjAyIENhc2ggUmVnaXN0ZXIgcmVzdG9yZWQ=\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "PRINT_NEXIST",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.155+0200",
          "notes": [

          ],
          "step_number": 75,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "UFJJTlQgMDIzMTU2OjxiPllvdXIgdGV4dCBoZXJlPC9iPg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C260_DATA_PRINTED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.169+0200",
          "notes": [

          ],
          "step_number": 76,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjYwIERhdGEgcHJpbnRlZA==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "OPEN",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.178+0200",
          "notes": [

          ],
          "step_number": 77,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "T1BFTg==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C231_ACCOUNT_OPENED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.183+0200",
          "notes": [

          ],
          "step_number": 78,
          "state_vector_size": 1,
          "advance_duration_ms": 3,
          "physical_label": "MjMxIDAwMDIwMSBBY2NvdW50IG9wZW5lZA==\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "RESUME",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.192+0200",
          "notes": [

          ],
          "step_number": 79,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "UkVTVU1F\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C201_RESUMED_OPERATION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.198+0200",
          "notes": [

          ],
          "step_number": 80,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjAxIFJlc3VtZWQgb3BlcmF0aW9u\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "ARTREG_EXIST",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.205+0200",
          "notes": [

          ],
          "step_number": 81,
          "state_vector_size": 3,
          "advance_duration_ms": 3,
          "physical_label": "QVJUUkVHIDIwMDk3MDA0\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C212_ART_DESCRIPTION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.213+0200",
          "notes": [

          ],
          "step_number": 82,
          "state_vector_size": 2,
          "advance_duration_ms": 2,
          "physical_label": "MjEyIExvcmQgTmVsc29uIHRlYToxLDE2\n",
          "label_parameters": {
            "_price": "1.16"
          }
        },
        {
          "label": {
            "name": "C232_ART_REGISTERED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.249+0200",
          "notes": [

          ],
          "step_number": 83,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjMyIDE6MSwxNiBBcnRpY2xlIHJlZ2lzdGVyZWQ=\n",
          "label_parameters": {
            "_total": "1.16"
          }
        },
        {
          "label": {
            "name": "ARTREG",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.260+0200",
          "notes": [

          ],
          "step_number": 84,
          "state_vector_size": 3,
          "advance_duration_ms": 3,
          "physical_label": "QVJUUkVH\n",
          "label_parameters": {
          }
        },
        {
          "label": {
            "name": "C212_ART_DESCRIPTION",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.268+0200",
          "notes": [

          ],
          "step_number": 85,
          "state_vector_size": 2,
          "advance_duration_ms": 1,
          "physical_label": "MjEyIExvcmQgTmVsc29uIHRlYToxLDE2\n",
          "label_parameters": {
            "_price": "1.16"
          }
        },
        {
          "label": {
            "name": "C232_ART_REGISTERED",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.305+0200",
          "notes": [

          ],
          "step_number": 86,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "MjMyIDE6MSwxNiBBcnRpY2xlIHJlZ2lzdGVyZWQ=\n",
          "label_parameters": {
            "_total": "1.16"
          }
        },
        {
          "label": {
            "name": "GET",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.313+0200",
          "notes": [

          ],
          "step_number": 87,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "R0VUIEFJX1NUQU1Q\n",
          "label_parameters": {
            "get_variable_name": "AI_STAMP"
          }
        },
        {
          "label": {
            "name": "C210_VAR_RETURN",
            "direction": "response",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.322+0200",
          "notes": [

          ],
          "step_number": 88,
          "state_vector_size": 2,
          "advance_duration_ms": 3,
          "physical_label": "MjEwIEFJX1NUQU1QOjA6MCwwMA==\n",
          "label_parameters": {
            "return_variable_name": "AI_STAMP"
          }
        },
        {
          "label": {
            "name": "ARTREG_EXIST",
            "direction": "stimulus",
            "channel": "pos"
          },
          "timestamp": "2018-03-29T03:01:37.331+0200",
          "notes": [

          ],
          "step_number": 89,
          "state_vector_size": 3,
          "advance_duration_ms": 3,
          "physical_label": "QVJUUkVHIDIwMDEzMjI2\n",
          "label_parameters": {
          }
        }
      ],
      "last_step": 89,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 2,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.540+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 3,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.596+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 4,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.641+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 5,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.690+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 6,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.738+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 7,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.785+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 8,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.831+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 9,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.879+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 10,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.925+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 11,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:37.973+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 12,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.023+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 13,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.069+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 14,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.116+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 15,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.162+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 16,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.210+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 17,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.256+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 18,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.305+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 19,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.354+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 20,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.402+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 21,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.454+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 22,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.500+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 23,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.546+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 24,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.594+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 25,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.641+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 26,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.689+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 27,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.736+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 28,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.791+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 29,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.845+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    },
    {
      "index": 30,
      "verdict": "error",
      "error_message": "Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801",
      "steps": [
        {
          "label": {
            "name": "error",
            "direction": "response",
            "channel": "error"
          },
          "timestamp": "2018-03-29T03:01:38.900+0200",
          "notes": [

          ],
          "step_number": 0,
          "state_vector_size": null,
          "advance_duration_ms": null,
          "physical_label": null,
          "label_parameters": {
          }
        }
      ],
      "last_step": 0,
      "expected_labels": [

      ],
      "tags": [

      ]
    }
  ]
}
			""".trimIndent())
		storageService.addAllPassingTests(testRun.testCases)
		val arffString = """
@relation testcases_steps

@attribute !error numeric
@attribute '?GET if ((get_variable_name == \\\"CS_EVENT\\\"))' numeric
@attribute '!C210_VAR_RETURN if ((return_variable_name == \\\"CS_EVENT\\\") && (variable_value == \\\"EV_NONE\\\"))' numeric
@attribute !C251_SIGNED_ON numeric
@attribute !C201_RESUMED_OPERATION numeric
@attribute ?RESETCR numeric
@attribute '!C210_VAR_RETURN if ((return_variable_name == \\\"CS_ACCNT\\\") && (variable_value == \\\"AS_IDLE\\\"))' numeric
@attribute !C501_SYNTAX_ERROR numeric
@attribute !C220_SCRP_READY numeric
@attribute ?RESUME numeric
@attribute !C250_SIGNED_OFF numeric
@attribute '!C210_VAR_RETURN if ((return_variable_name == \\\"CS_SIGN\\\") && (variable_value == \\\"SS_OFF\\\"))' numeric
@attribute ?PRINT_NEXIST numeric
@attribute '?GET if ((get_variable_name == \\\"AI_STAMP\\\"))' numeric
@attribute '?GET if ((get_variable_name == \\\"CS_SIGN\\\"))' numeric
@attribute ?SIGNOFF numeric
@attribute '!C210_VAR_RETURN if ((return_variable_name == \\\"AI_STAMP\\\"))' numeric
@attribute !C202_CASH_REGISTER_RESTORED numeric
@attribute !C231_ACCOUNT_OPENED numeric
@attribute '!C212_ART_DESCRIPTION if ((_price == \\\"1.16\\\"))' numeric
@attribute '!C210_VAR_RETURN if ((return_variable_name == \\\"CS_SIGN\\\") && (variable_value == \\\"SS_ON\\\"))' numeric
@attribute ?ARTID_WEIGHTED numeric
@attribute ?SIGNON_EXIST numeric
@attribute ?ARTREG_EXIST numeric
@attribute '?GET if ((get_variable_name == \\\"CS_ACCNT\\\"))' numeric
@attribute !C260_DATA_PRINTED numeric
@attribute ?OPEN numeric
@attribute ?ARTREG numeric
@attribute !C213_ART_TEXT numeric
@attribute '!C232_ART_REGISTERED if ((_total == \\\"1.16\\\"))' numeric

@data
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
		""".trimIndent()
		val arff = ArffLoader.ArffReader(StringReader(arffString)).data
		assert(storageService.instances.equalHeaders(arff))
		assert(storageService.instances eq arff)

	}
}

infix fun Instances.eq(other: Instances): Boolean{
	val thisDoubles = this.map { it.toDoubleArray().joinToString() }
	val otherDoubles = other.map { it.toDoubleArray().joinToString() }
	for (doubleArray in thisDoubles){
		if (doubleArray !in otherDoubles){
			return false
		}
	}
	return thisDoubles.size == otherDoubles.size
}