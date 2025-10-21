import React, { useEffect, useState } from 'react';

import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  NativeEventEmitter,
  NativeModules,
} from 'react-native';
import {
  openNavigationScreen,
  setNavigationOptions,
} from '@basarsoft/react-native-yolbilnavigationui';

export default function App() {
  useEffect(() => {
    setNavigationOptions(
      // true
      {
        bmsAppCode: 'appCode',
        bmsAccountId: 'accountId',
        bmsServiceUrl: 'https://bms.basarsoft.com.tr/service', //Varsayılan Değer: "https://bms.basarsoft.com.tr/service"
        mockGps: true, // string or undefined
        reCalculateRouteEnabled: true,
        useLiveTraffic: true, //Varsayılan Değer: false
        snapMaxDistance: 500, //Varsayılan Değer: 500
        costType: 'Fastest', //Varsayılan Değer: Fastest
        navigationMode: 'Car', //Varsayılan Değer: Car
        avoidToolRoad: false, //Varsayılan Değer: false
        avoidHighway: false, //Varsayılan Değer: false
        avoidPrivateRoad: false, //Varsayılan Değer: false
        avoidRestrictedRoad: false, //Varsayılan Değer: false
        useFerry: false, //Varsayılan Değer: false
        useBoat: false, //Varsayılan Değer: false
        voiceEnabled: true, //Varsayılan Değer: true
        reCalculateDistance: 50, //Varsayılan Değer: 50
        leaveFromRouteAlertDistance: 35, //Varsayılan Değer: 35
      }
    ).then((res) => console.log('set options res= ' + res));
  }, []);

  useEffect(() => {
    let eventListener = [];
    const eventEmitter = new NativeEventEmitter(
      NativeModules.YolbilnavigationuiModule
    );
    eventListener.push(
      eventEmitter.addListener('onReady', (event) => {
        console.log(event.eventProperty); // "someValue"
        console.log(event.distance); // "someValue"
        console.log(event.duration); // "someValue"
      })
    );
    eventListener.push(
      eventEmitter.addListener('onRemainingDistanceChange', (event) => {
        console.log(event.eventProperty); // "someValue"
        console.log(event.distance); // "someValue"
        console.log(event.duration); // "someValue"
      })
    );
    eventListener.push(
      eventEmitter.addListener('onStart', (event) => {
        console.log(event.eventProperty); // "someValue"
      })
    );
    eventListener.push(
      eventEmitter.addListener('onFinish', (event) => {
        console.log(event.eventProperty); // "someValue"
        console.log(event.startTime);
        console.log(event.origin);
        console.log(event.destination);
        console.log(event.startTime);
        console.log(event.finishTime);
        console.log(event.calculatedRouteDuration);
        console.log(event.calculatedRouteDistance);
        console.log(event.userRouteDuration);
        console.log(event.userRouteDistance);
        console.log(event.routeCalculationCount);
      })
    );
    eventListener.push(
      eventEmitter.addListener('onStop', (event) => {
        console.log(event.eventProperty); // "someValue"
        console.log(event.startTime);
        console.log(event.origin);
        console.log(event.destination);
        console.log(event.startTime);
        console.log(event.finishTime);
        console.log(event.calculatedRouteDuration);
        console.log(event.calculatedRouteDistance);
        console.log(event.userRouteDuration);
        console.log(event.userRouteDistance);
        console.log(event.routeCalculationCount);
      })
    );
    eventListener.push(
      eventEmitter.addListener('onRouteRecalculate', (event) => {
        console.log(event.eventProperty); // "someValue"
      })
    );
    eventListener.push(
      eventEmitter.addListener('onLeaveFromRoute', (event) => {
        console.log(event.longitude); // "someValue"
        console.log(event.latitude); // "someValue"
        console.log(event.accuracy); // "someValue"
        console.log(event.speed); // "someValue"
        console.log(event.eventProperty); // "someValue"
      })
    );
    eventListener.push(
      eventEmitter.addListener('onException', (event) => {
        console.log(event.message); // "someValue"
        console.log(event.eventProperty); // "someValue"
      })
    );
    eventListener.push(
      eventEmitter.addListener('onLocationChange', (event) => {
        console.log(event.longitude); // "someValue"
        console.log(event.latitude); // "someValue"
        console.log(event.accuracy); // "someValue"
        console.log(event.speed); // "someValue"
        console.log(event.eventProperty); // "someValue"
      })
    );

    return () => {
      eventListener.forEach((event) => {
        event.remove();
      });
      eventListener = [];
      //Removes the listener
    };
  }, []);

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={styles.touchOp}
        onPress={() => {
          openNavigationScreen({ lat: 39.8902, lon: 32.82435 }).then((res) =>
            console.log('open screen res= ' + res)
          );
        }}
      >
        <Text>From your location</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={{ ...styles.touchOp, backgroundColor: 'yellow' }}
        onPress={() => {
          openNavigationScreen(
            { lat: 39.8, lon: 32.9 },
            { lat: 39.90042004801428, lon: 32.775201454708245 }
          ).then((res) => console.log('open screen res= ' + res));
        }}
      >
        <Text>From another location</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  touchOp: {
    height: 30,
    width: 200,
    margin: 30,
    backgroundColor: 'lightblue',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
