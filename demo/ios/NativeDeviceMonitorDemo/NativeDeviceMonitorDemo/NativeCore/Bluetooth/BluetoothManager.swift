//
//  BluetoothManager.swift
//  NativeDeviceMonitorDemo
//
//  Created by synguyen on 17/9/26.
//

import Foundation
import CoreBluetooth

public struct BleDevice: Identifiable, Equatable {
    public let id: UUID
    public let name: String
    public let rssi: Int
    public var peripheral: CBPeripheral?
    
    public init(id: UUID, name: String, rssi: Int, peripheral: CBPeripheral? = nil) {
        self.id = id
        self.name = name
        self.rssi = rssi
        self.peripheral = peripheral
    }
    
    public static func == (lhs: BleDevice, rhs: BleDevice) -> Bool {
        lhs.id == rhs.id
    }
}

public enum ConnectionState {
    case disconnected
    case connecting
    case connected
    case disconnecting
}

public struct BleCharacteristic: Identifiable {
    public let id: CBUUID
    public let properties: CBCharacteristicProperties
    public let isNotifying: Bool
    
    public init(id: CBUUID, properties: CBCharacteristicProperties, isNotifying: Bool) {
        self.id = id
        self.properties = properties
        self.isNotifying = isNotifying
    }
}

public struct BleService: Identifiable {
    public let id: CBUUID
    public let characteristics: [BleCharacteristic]
    
    public init(id: CBUUID, characteristics: [BleCharacteristic]) {
        self.id = id
        self.characteristics = characteristics
    }
}

public protocol BluetoothManagerProtocol {
    func isAvailable() -> Bool
    func isEnabled() -> Bool
    func startScan(onDeviceFound: @escaping (BleDevice) -> Void, onError: @escaping (String) -> Void)
    func stopScan()
    func connect(peripheralId: UUID, onStateChange: @escaping (ConnectionState) -> Void)
    func disconnect()
    func discoverServices(onSuccess: @escaping ([BleService]) -> Void, onError: @escaping (String) -> Void)
    func read(characteristicUuid: CBUUID, completion: @escaping (Data?, Error?) -> Void)
    func subscribe(characteristicUuid: CBUUID, onNotification: @escaping (Data) -> Void)
    func unsubscribe(characteristicUuid: CBUUID)
    func getConnectionState() -> ConnectionState
}

public class BluetoothManager: NSObject, BluetoothManagerProtocol, CBCentralManagerDelegate, CBPeripheralDelegate {
    
    public static let shared = BluetoothManager()
    
    private var centralManager: CBCentralManager!
    private var discoveredPeripherals: [UUID: CBPeripheral] = [:]
    private var connectedPeripheral: CBPeripheral?
    private var activeCharacteristics: [CBUUID: CBCharacteristic] = [:]
    
    // Callbacks
    private var onDeviceFound: ((BleDevice) -> Void)?
    private var onScanError: ((String) -> Void)?
    private var onConnectionStateChange: ((ConnectionState) -> Void)?
    private var onServicesDiscovered: (([BleService]) -> Void)?
    private var onServicesError: ((String) -> Void)?
    private var readCallbacks: [CBUUID: (Data?, Error?) -> Void] = [:]
    private var notificationCallbacks: [CBUUID: (Data) -> Void] = [:]
    
    private var connectionState: ConnectionState = .disconnected {
        didSet {
            let state = connectionState
            DispatchQueue.main.async { [weak self] in
                self?.onConnectionStateChange?(state)
            }
        }
    }
    
    public override init() {
        super.init()
        self.centralManager = CBCentralManager(delegate: self, queue: .main)
    }
    
    // MARK: - BluetoothManagerProtocol
    
    public func isAvailable() -> Bool {
        return centralManager.state != .unsupported
    }
    
    public func isEnabled() -> Bool {
        return centralManager.state == .poweredOn
    }
    
    public func startScan(onDeviceFound: @escaping (BleDevice) -> Void, onError: @escaping (String) -> Void) {
        self.onDeviceFound = onDeviceFound
        self.onScanError = onError
        
        guard centralManager.state == .poweredOn else {
            onError("Bluetooth is not turned on (State: \(centralManager.state.rawValue))")
            return
        }
        
        discoveredPeripherals.removeAll()
        centralManager.scanForPeripherals(
            withServices: nil,
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
    }
    
    public func stopScan() {
        centralManager.stopScan()
        onDeviceFound = nil
        onScanError = nil
    }
    
    public func connect(peripheralId: UUID, onStateChange: @escaping (ConnectionState) -> Void) {
        self.onConnectionStateChange = onStateChange
        
        guard let peripheral = discoveredPeripherals[peripheralId] else {
            onStateChange(.disconnected)
            return
        }
        
        connectedPeripheral = peripheral
        peripheral.delegate = self
        connectionState = .connecting
        centralManager.connect(peripheral, options: nil)
    }
    
    public func disconnect() {
        guard let peripheral = connectedPeripheral else { return }
        connectionState = .disconnecting
        centralManager.cancelPeripheralConnection(peripheral)
    }
    
    public func discoverServices(onSuccess: @escaping ([BleService]) -> Void, onError: @escaping (String) -> Void) {
        guard let peripheral = connectedPeripheral, connectionState == .connected else {
            onError("No device currently connected.")
            return
        }
        self.onServicesDiscovered = onSuccess
        self.onServicesError = onError
        peripheral.discoverServices(nil)
    }
    
    public func read(characteristicUuid: CBUUID, completion: @escaping (Data?, Error?) -> Void) {
        guard let peripheral = connectedPeripheral,
              let characteristic = activeCharacteristics[characteristicUuid] else {
            completion(nil, NSError(domain: "BluetoothManager", code: -1, userInfo: [NSLocalizedDescriptionKey: "Characteristic not found"]))
            return
        }
        readCallbacks[characteristicUuid] = completion
        peripheral.readValue(for: characteristic)
    }
    
    public func subscribe(characteristicUuid: CBUUID, onNotification: @escaping (Data) -> Void) {
        guard let peripheral = connectedPeripheral,
              let characteristic = activeCharacteristics[characteristicUuid] else {
            return
        }
        notificationCallbacks[characteristicUuid] = onNotification
        peripheral.setNotifyValue(true, for: characteristic)
    }
    
    public func unsubscribe(characteristicUuid: CBUUID) {
        guard let peripheral = connectedPeripheral,
              let characteristic = activeCharacteristics[characteristicUuid] else {
            return
        }
        notificationCallbacks.removeValue(forKey: characteristicUuid)
        peripheral.setNotifyValue(false, for: characteristic)
    }
    
    public func getConnectionState() -> ConnectionState {
        return connectionState
    }
    
    // MARK: - CBCentralManagerDelegate
    
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state != .poweredOn && connectionState != .disconnected {
            connectionState = .disconnected
        }
    }
    
    public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String: Any], rssi RSSI: NSNumber) {
        discoveredPeripherals[peripheral.identifier] = peripheral
        
        let localName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        let deviceName = localName ?? peripheral.name ?? "Unknown Device"
        let bleDevice = BleDevice(
            id: peripheral.identifier,
            name: deviceName,
            rssi: RSSI.intValue,
            peripheral: peripheral
        )
        
        DispatchQueue.main.async { [weak self] in
            self?.onDeviceFound?(bleDevice)
        }
    }
    
    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        connectionState = .connected
    }
    
    public func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        connectionState = .disconnected
        connectedPeripheral = nil
    }
    
    public func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        connectionState = .disconnected
        connectedPeripheral = nil
        activeCharacteristics.removeAll()
    }
    
    // MARK: - CBPeripheralDelegate
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error = error {
            DispatchQueue.main.async { [weak self] in
                self?.onServicesError?(error.localizedDescription)
            }
            return
        }
        
        guard let services = peripheral.services, !services.isEmpty else {
            DispatchQueue.main.async { [weak self] in
                self?.onServicesDiscovered?([])
            }
            return
        }
        
        for service in services {
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let services = peripheral.services else { return }
        
        for s in services {
            for c in s.characteristics ?? [] {
                activeCharacteristics[c.uuid] = c
            }
        }
        
        let bleServices: [BleService] = services.map { s in
            let chars = (s.characteristics ?? []).map { c in
                BleCharacteristic(id: c.uuid, properties: c.properties, isNotifying: c.isNotifying)
            }
            return BleService(id: s.uuid, characteristics: chars)
        }
        
        DispatchQueue.main.async { [weak self] in
            self?.onServicesDiscovered?(bleServices)
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if let readCallback = readCallbacks.removeValue(forKey: characteristic.uuid) {
            DispatchQueue.main.async {
                readCallback(characteristic.value, error)
            }
        }
        
        if let data = characteristic.value, let notifyCallback = notificationCallbacks[characteristic.uuid] {
            DispatchQueue.main.async {
                notifyCallback(data)
            }
        }
    }
}
